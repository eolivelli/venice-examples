package org.example.simplewriter;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.ControllerResponse;
import com.linkedin.venice.controllerapi.MultiSchemaResponse;
import com.linkedin.venice.controllerapi.SchemaResponse;
import com.linkedin.venice.controllerapi.StoreResponse;
import com.linkedin.venice.controllerapi.VersionCreationResponse;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.partitioner.VenicePartitioner;
import com.linkedin.venice.pushmonitor.ExecutionStatus;
import com.linkedin.venice.pushmonitor.HybridStoreQuotaStatus;
import com.linkedin.venice.samza.SamzaExitMode;
import com.linkedin.venice.security.SSLFactory;
import com.linkedin.venice.serialization.avro.VeniceAvroKafkaSerializer;
import com.linkedin.venice.utils.BoundedHashMap;
import com.linkedin.venice.utils.Pair;
import com.linkedin.venice.utils.PartitionUtils;
import com.linkedin.venice.utils.SystemTime;
import com.linkedin.venice.utils.Time;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.utils.VeniceProperties;
import com.linkedin.venice.utils.concurrent.VeniceConcurrentHashMap;
import com.linkedin.venice.writer.CompletableFutureCallback;
import com.linkedin.venice.writer.VeniceWriter;
import com.linkedin.venice.writer.VeniceWriterFactory;
import com.linkedin.venice.writer.VeniceWriterOptions;
import lombok.Getter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.samza.SamzaException;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemProducer;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static com.linkedin.venice.ConfigKeys.KAFKA_BOOTSTRAP_SERVERS;
import static com.linkedin.venice.schema.AvroSchemaParseUtils.parseSchemaFromJSONLooseValidation;
import static com.linkedin.venice.schema.AvroSchemaParseUtils.parseSchemaFromJSONStrictValidation;

public class VeniceSystemProducer implements SystemProducer, Closeable {
  private static final Logger LOGGER = LogManager.getLogger(VeniceSystemProducer.class);

  private static final Schema STRING_SCHEMA = Schema.create(Schema.Type.STRING);
  private static final Schema INT_SCHEMA = Schema.create(Schema.Type.INT);
  private static final Schema LONG_SCHEMA = Schema.create(Schema.Type.LONG);
  private static final Schema FLOAT_SCHEMA = Schema.create(Schema.Type.FLOAT);
  private static final Schema DOUBLE_SCHEMA = Schema.create(Schema.Type.DOUBLE);
  private static final Schema BYTES_SCHEMA = Schema.create(Schema.Type.BYTES);
  private static final Schema BOOL_SCHEMA = Schema.create(Schema.Type.BOOLEAN);
  private static final DatumWriter<Utf8> STRING_DATUM_WRITER = new GenericDatumWriter<>(STRING_SCHEMA);
  private static final DatumWriter<Integer> INT_DATUM_WRITER = new GenericDatumWriter<>(INT_SCHEMA);
  private static final DatumWriter<Long> LONG_DATUM_WRITER = new GenericDatumWriter<>(LONG_SCHEMA);
  private static final DatumWriter<Float> FLOAT_DATUM_WRITER = new GenericDatumWriter<>(FLOAT_SCHEMA);
  private static final DatumWriter<Double> DOUBLE_DATUM_WRITER = new GenericDatumWriter<>(DOUBLE_SCHEMA);
  private static final DatumWriter<ByteBuffer> BYTES_DATUM_WRITER = new GenericDatumWriter<>(BYTES_SCHEMA);
  private static final DatumWriter<Boolean> BOOL_DATUM_WRITER = new GenericDatumWriter<>(BOOL_SCHEMA);

  // Immutable state
  private final String storeName;
  private final String samzaJobId;
  private final Version.PushType pushType;
  private final Optional<SSLFactory> sslFactory;
  private final VeniceSystemFactory factory;
  private final Optional<String> partitioners;
  private final Time time;
  private final String runningFabric;
  @Getter
  private final VeniceConcurrentHashMap<Schema, Pair<Integer, Integer>> valueSchemaIds =
      new VeniceConcurrentHashMap<>();
  /**
   * key is schema
   * value is Avro serializer
   */
  private final Map<String, VeniceAvroKafkaSerializer> serializers = new VeniceConcurrentHashMap<>();

  // Mutable, lazily initialized, state
  @Getter
  private Schema keySchema;
  private String canonicalKeySchemaStr;
  // To avoid the excessive usage of the cache in case each message is using a unique key schema
  private final Map<Schema, String> canonicalSchemaStrCache = new BoundedHashMap<>(10, true);

  private ControllerClient controllerClient;
  // It can be version topic, real-time topic or stream reprocessing topic, depending on push type
  private String topicName;
  private String kafkaBootstrapServers;

  private boolean isWriteComputeEnabled = false;
  private boolean isChunkingEnabled = false;

  private boolean isStarted = false;

  private VeniceWriter<byte[], byte[], byte[]> veniceWriter = null;
  private Optional<RouterBasedPushMonitor> pushMonitor = Optional.empty();
  private Optional<RouterBasedHybridStoreQuotaMonitor> hybridStoreQuotaMonitor = Optional.empty();

  /**
   * Construct a new instance of {@link VeniceSystemProducer}.
   */
  public VeniceSystemProducer(
      String storeName,
      Version.PushType pushType,
      String samzaJobId,
      String runningFabric,
      VeniceSystemFactory factory,
      Optional<SSLFactory> sslFactory,
      Optional<String> partitioners) {
    this(
        storeName,
        pushType,
        samzaJobId,
        runningFabric,
        factory,
        sslFactory,
        partitioners,
        SystemTime.INSTANCE);
  }

  /**
   * Construct a new instance of {@link VeniceSystemProducer}
   * @param storeName The store to write to
   * @param pushType The {@link Version.PushType} to use to write to the store
   * @param samzaJobId A unique id used to identify jobs that can concurrently write to the same store
   * @param runningFabric The colo where the job is running. It is used to find the best destination for the data to be written to
   * @param factory The {@link VeniceSystemFactory} object that was used to create this object
   * @param sslFactory An optional {@link SSLFactory} that is used to communicate with other components using SSL
   * @param partitioners A list of comma-separated partitioners class names that are supported.
   * @param time An object of type {@link Time}. It is helpful to be configurable for testing.
   */
  public VeniceSystemProducer(
      String storeName,
      Version.PushType pushType,
      String samzaJobId,
      String runningFabric,
      VeniceSystemFactory factory,
      Optional<SSLFactory> sslFactory,
      Optional<String> partitioners,
      Time time) {
    this.storeName = storeName;
    this.pushType = pushType;
    this.samzaJobId = samzaJobId;
    this.runningFabric = runningFabric;
    this.factory = factory;
    this.sslFactory = sslFactory;
    this.partitioners = partitioners;
    this.time = time;
  }

  public String getRunningFabric() {
    return this.runningFabric;
  }

  protected ControllerResponse controllerRequestWithRetry(Supplier<ControllerResponse> supplier, int retryLimit) {
    String errorMsg = "";
    Exception lastException = null;
    for (int currentAttempt = 0; currentAttempt < retryLimit; currentAttempt++) {
      lastException = null;
      try {
        ControllerResponse controllerResponse = supplier.get();
        if (!controllerResponse.isError()) {
          return controllerResponse;
        } else {
          time.sleep(1000L * (currentAttempt + 1));
          errorMsg = controllerResponse.getError();
        }
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          throw new VeniceException(e);
        }
        try {
          time.sleep(1000L * (currentAttempt + 1));
        } catch (InterruptedException ie) {
          throw new VeniceException(ie);
        }
        errorMsg = e.getMessage();
        lastException = e;
      }
    }
    throw new SamzaException("Failed to send request to Controller, error: " + errorMsg, lastException);
  }

  /**
   * This method is overridden and not used by LinkedIn internally.
   * Please update the overridden method accordingly after modifying this method.
   */
  protected VeniceWriter<byte[], byte[], byte[]> getVeniceWriter(VersionCreationResponse store) {
    Properties veniceWriterProperties = new Properties();
    veniceWriterProperties.put(KAFKA_BOOTSTRAP_SERVERS, store.getKafkaBootstrapServers());
    return getVeniceWriter(store, veniceWriterProperties);
  }

  /**
   * Please don't remove this method since it is still being used by LinkedIn internally.
   */
  protected VeniceWriter<byte[], byte[], byte[]> getVeniceWriter(
      VersionCreationResponse store,
      Properties veniceWriterProperties) {
    int amplificationFactor = store.getAmplificationFactor();
    Optional<Integer> partitionCount = pushType.isBatchOrStreamReprocessing()
        ? Optional.of(store.getPartitions() * amplificationFactor)
        /**
         * N.B. There is an issue in the controller where the partition count inside a {@link VersionCreationResponse}
         *      for a non-batch topic is invalid, so in that case we don't rely on it, and let the {@link VeniceWriter}
         *      figure it out by doing a metadata call to Kafka. It would be great to fix the controller bug and then
         *      always pass in the partition count here, so we can skip this MD call.
         */
        : Optional.empty();
    Properties partitionerProperties = new Properties();
    partitionerProperties.putAll(store.getPartitionerParams());
    VenicePartitioner venicePartitioner = PartitionUtils.getVenicePartitioner(
        store.getPartitionerClass(),
        amplificationFactor,
        new VeniceProperties(partitionerProperties));
    VeniceWriterOptions writerOptions = new VeniceWriterOptions.Builder(store.getKafkaTopic()).setTime(time)
            .setPartitioner(venicePartitioner)
            .setPartitionCount(partitionCount.orElse(null))
            .setChunkingEnabled(isChunkingEnabled)
            .build();
    return new VeniceWriterFactory(veniceWriterProperties)
        .createVeniceWriter(writerOptions);
  }

  @Override
  public synchronized void start() {
    if (this.isStarted) {
      return;
    }
    this.isStarted = true;
    this.controllerClient = ControllerClient
            .discoverAndConstructControllerClient(storeName, "http://venice-controller:5555",
                    sslFactory, 1);

    // Request all the necessary info from Venice Controller
    VersionCreationResponse versionCreationResponse = (VersionCreationResponse) controllerRequestWithRetry(
        () -> this.controllerClient.requestTopicForWrites(
            this.storeName,
            1,
            pushType,
            samzaJobId,
            true, // sendStartOfPush must be true in order to support batch push to Venice from Samza app
            false, // Samza jobs, including batch ones, are expected to write data out of order
            false,
            partitioners,
            Optional.empty(),
            Optional.ofNullable(runningFabric),
            false,
            -1),
        2);
    LOGGER.info("Got [store: {}] VersionCreationResponse: {}", storeName, versionCreationResponse);
    this.topicName = versionCreationResponse.getKafkaTopic();
    this.kafkaBootstrapServers = versionCreationResponse.getKafkaBootstrapServers();

    StoreResponse storeResponse =
        (StoreResponse) controllerRequestWithRetry(() -> this.controllerClient.getStore(storeName), 2);
    this.isWriteComputeEnabled = storeResponse.getStore().isWriteComputationEnabled();

    boolean hybridStoreDiskQuotaEnabled = storeResponse.getStore().isHybridStoreDiskQuotaEnabled();

    SchemaResponse keySchemaResponse =
        (SchemaResponse) controllerRequestWithRetry(() -> this.controllerClient.getKeySchema(this.storeName), 2);
    LOGGER.info("Got [store: {}] SchemaResponse for key schema: {}", storeName, keySchemaResponse);
    this.keySchema = parseSchemaFromJSONStrictValidation(keySchemaResponse.getSchemaStr());
    this.canonicalKeySchemaStr = AvroCompatibilityHelper.toParsingForm(this.keySchema);

    MultiSchemaResponse valueSchemaResponse = (MultiSchemaResponse) controllerRequestWithRetry(
        () -> this.controllerClient.getAllValueAndDerivedSchema(this.storeName),
        2);
    LOGGER.info("Got [store: {}] SchemaResponse for value schemas: {}", storeName, valueSchemaResponse);
    for (MultiSchemaResponse.Schema valueSchema: valueSchemaResponse.getSchemas()) {
      valueSchemaIds.put(
          parseSchemaFromJSONLooseValidation(valueSchema.getSchemaStr()),
          new Pair<>(valueSchema.getId(), valueSchema.getDerivedSchemaId()));
    }

    if (pushType.equals(Version.PushType.STREAM_REPROCESSING)) {
      throw new UnsupportedOperationException();
    }

    if (pushType.isBatchOrStreamReprocessing()) {
      int versionNumber = versionCreationResponse.getVersion();
      Version version = storeResponse.getStore()
          .getVersion(versionNumber)
          .orElseThrow(
              () -> new VeniceException(
                  "Version info for version " + versionNumber + " not available in store response"));
      // For pushes made to VT or SR topic, the producer should chunk the data
      this.isChunkingEnabled = version.isChunkingEnabled();
    } else {
      // For pushes made to RT, the producer should not chunk the data
      this.isChunkingEnabled = false;
    }

    this.veniceWriter = getVeniceWriter(versionCreationResponse);

    if (pushMonitor.isPresent()) {
      /**
       * If the stream reprocessing job has finished, push monitor will exit the Samza process directly.
       */
      ExecutionStatus currentStatus = pushMonitor.get().getCurrentStatus();
      if (ExecutionStatus.ERROR.equals(currentStatus)) {
        throw new VeniceException(
            "Push job for resource " + topicName + " is in error state; please reach out to Venice team.");
      }
    }

    if ((pushType.equals(Version.PushType.STREAM) || pushType.equals(Version.PushType.STREAM_REPROCESSING))
        && hybridStoreDiskQuotaEnabled) {
     throw new UnsupportedOperationException();
    }
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public synchronized void stop() {
    this.isStarted = false;
    Utils.closeQuietlyWithErrorLogged(veniceWriter);
    if (Version.PushType.STREAM_REPROCESSING.equals(pushType) && pushMonitor.isPresent()) {
      String versionTopic = Version.composeVersionTopicFromStreamReprocessingTopic(topicName);
      switch (pushMonitor.get().getCurrentStatus()) {
        case COMPLETED:
          LOGGER.info("Push job for {} is COMPLETED.", topicName);
          break;
        case END_OF_PUSH_RECEIVED:
          LOGGER.info("Batch load for {} has finished.", topicName);
          break;
        case ERROR:
          LOGGER.info("Push job for {} encountered error.", topicName);
          break;
        default:
          LOGGER.warn("Push job in Venice backend is still in progress... Will clean up resources in Venice");
          /**
           * Consider there could be hundreds of Samza containers for stream reprocessing job, we shouldn't let all
           * the containers send kill requests to controller at the same time to avoid hammering on controller.
           */
          Utils.sleep(ThreadLocalRandom.current().nextInt(30000));
          controllerClient.retryableRequest(3, c -> c.killOfflinePushJob(versionTopic));
          LOGGER.info("Offline push job has been killed, topic: {}", versionTopic);
      }
      Utils.closeQuietlyWithErrorLogged(pushMonitor.get());
    }
    Utils.closeQuietlyWithErrorLogged(controllerClient);
    hybridStoreQuotaMonitor.ifPresent(Utils::closeQuietlyWithErrorLogged);
  }

  @Override
  public void register(String source) {

  }

  @Override
  public void send(String source, OutgoingMessageEnvelope outgoingMessageEnvelope) {
    String storeOfIncomingMessage = outgoingMessageEnvelope.getSystemStream().getStream();
    if (!storeOfIncomingMessage.equals(storeName)) {
      throw new SamzaException(
          "The store of the incoming message: " + storeOfIncomingMessage + " is unexpected, and it should be "
              + storeName);
    }

    if (pushMonitor.isPresent() && Version.PushType.STREAM_REPROCESSING.equals(pushType)) {
      ExecutionStatus currentStatus = pushMonitor.get().getCurrentStatus();
      switch (currentStatus) {
        case ERROR:
          /**
           * If there are multiple stream reprocessing SystemProducer in one Samza job, one failed push will
           * also affect other push jobs.
           */
          throw new VeniceException(
              "Push job for resource " + topicName + " is in error state; please reach out to Venice team.");
        case END_OF_PUSH_RECEIVED:
        case COMPLETED:
          LOGGER.info("Stream reprocessing for resource {} has finished. No message will be sent.", topicName);
          return;
        default:
          // no-op
      }
    }
    if (hybridStoreQuotaMonitor.isPresent()
        && (Version.PushType.STREAM.equals(pushType) || Version.PushType.STREAM_REPROCESSING.equals(pushType))) {
      HybridStoreQuotaStatus currentStatus = hybridStoreQuotaMonitor.get().getCurrentStatus();
      switch (currentStatus) {
        case QUOTA_VIOLATED:
          /**
           * If there are multiple stream SystemProducer in one Samza job, one failed push will
           * also affect other push jobs.
           */
          LOGGER.error("Current hybrid store quota status: {}, should throw exception to kill the job.", currentStatus);
          throw new VeniceException(
              "Push job for resource " + topicName
                  + " is in hybrid quota violated mode; please reach out to Venice team.");
        case QUOTA_NOT_VIOLATED:
        default:
          // no-op
      }
    }

    send(outgoingMessageEnvelope.getKey(), outgoingMessageEnvelope.getMessage());
  }

  protected CompletableFuture<Void> send(Object keyObject, Object valueObject) {
    Schema keyObjectSchema = getSchemaFromObject(keyObject);
    String canonicalSchemaStr = canonicalSchemaStrCache
        .computeIfAbsent(keyObjectSchema, k -> AvroCompatibilityHelper.toParsingForm(keyObjectSchema));

    if (!canonicalKeySchemaStr.equals(canonicalSchemaStr)) {
      throw new SamzaException(
          "Cannot write record to Venice store " + storeName + ", key object has schema " + canonicalSchemaStr
              + " which does not match Venice key schema " + canonicalKeySchemaStr + ".");
    }

    byte[] key = serializeObject(topicName, keyObject);
    final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
    final CompletableFutureCallback callback = new CompletableFutureCallback(completableFuture);

    long logicalTimestamp = -1;
    if (valueObject instanceof VeniceObjectWithTimestamp) {
      VeniceObjectWithTimestamp objectWithTimestamp = (VeniceObjectWithTimestamp) valueObject;
      logicalTimestamp = objectWithTimestamp.getTimestamp();
      if (logicalTimestamp <= 0) {
        throw new SamzaException(
            "Timestamp specified in passed `VeniceObjectWithTimestamp` object should be positive, but received: "
                + logicalTimestamp);
      }
      valueObject = objectWithTimestamp.getObject();
    }

    if (valueObject == null) {
      if (logicalTimestamp > 0) {
        veniceWriter.delete(key, logicalTimestamp, callback);
      } else {
        veniceWriter.delete(key, callback);
      }
    } else {
      Schema valueObjectSchema = getSchemaFromObject(valueObject);

      Pair<Integer, Integer> valueSchemaIdPair = valueSchemaIds.computeIfAbsent(valueObjectSchema, valueSchema -> {
        SchemaResponse valueSchemaResponse = (SchemaResponse) controllerRequestWithRetry(
            () -> controllerClient.getValueOrDerivedSchemaId(storeName, valueSchema.toString()),
            2);
        LOGGER.info("Got [store: {}] SchemaResponse for schema: {}", storeName, valueSchema);
        return new Pair<>(valueSchemaResponse.getId(), valueSchemaResponse.getDerivedSchemaId());
      });

      byte[] value = serializeObject(topicName, valueObject);

      if (valueSchemaIdPair.getSecond() == -1) {
        if (logicalTimestamp > 0) {
          veniceWriter.put(key, value, valueSchemaIdPair.getFirst(), logicalTimestamp, callback);
        } else {
          veniceWriter.put(key, value, valueSchemaIdPair.getFirst(), callback);
        }
      } else {
        if (!isWriteComputeEnabled) {
          throw new SamzaException(
              "Cannot write partial update record to Venice store " + storeName + " "
                  + "because write-compute is not enabled for it. Please contact Venice team to configure it.");
        }
        if (logicalTimestamp > 0) {
          veniceWriter.update(
              key,
              value,
              valueSchemaIdPair.getFirst(),
              valueSchemaIdPair.getSecond(),
              callback,
              logicalTimestamp);
        } else {
          veniceWriter.update(key, value, valueSchemaIdPair.getFirst(), valueSchemaIdPair.getSecond(), callback);
        }
      }
    }
    return completableFuture;
  }

  public CompletableFuture<Void> put(Object keyObject, Object valueObject) {
    return send(keyObject, valueObject);
  }

  public CompletableFuture<Void> delete(Object keyObject) {
    return send(keyObject, null);
  }

  /**
   * Flushing the data to Venice store in case VeniceSystemProducer buffers message.
   *
   * @param s String representing the source of the message. Currently, VeniceSystemProducer is not using this param.
   */
  @Override
  public void flush(String s) {
    veniceWriter.flush();
  }

  private static Schema getSchemaFromObject(Object object) {
    if (object instanceof IndexedRecord) {
      IndexedRecord keyAvro = (IndexedRecord) object;
      return keyAvro.getSchema();
    } else if (object instanceof CharSequence) { // convenience option.
      return STRING_SCHEMA;
    } else if (object instanceof Integer) {
      return INT_SCHEMA;
    } else if (object instanceof Long) {
      return LONG_SCHEMA;
    } else if (object instanceof Double) {
      return DOUBLE_SCHEMA;
    } else if (object instanceof Float) {
      return FLOAT_SCHEMA;
    } else if (object instanceof byte[] || object instanceof ByteBuffer) {
      return BYTES_SCHEMA;
    } else if (object instanceof Boolean) {
      return BOOL_SCHEMA;
    } else {
      throw new SamzaException(
          "Venice System Producer only supports Avro objects, and primitives, found object of class: "
              + object.getClass().toString());
    }
  }

  private byte[] serializeObject(String topic, Object input) {
    if (input instanceof IndexedRecord) {
      VeniceAvroKafkaSerializer serializer =
          serializers.computeIfAbsent(((IndexedRecord) input).getSchema().toString(), VeniceAvroKafkaSerializer::new);
      return serializer.serialize(topic, input);
    } else if (input instanceof CharSequence) {
      return serializePrimitive(new Utf8(input.toString()), STRING_DATUM_WRITER);
    } else if (input instanceof Integer) {
      return serializePrimitive((Integer) input, INT_DATUM_WRITER);
    } else if (input instanceof Long) {
      return serializePrimitive((Long) input, LONG_DATUM_WRITER);
    } else if (input instanceof Double) {
      return serializePrimitive((Double) input, DOUBLE_DATUM_WRITER);
    } else if (input instanceof Float) {
      return serializePrimitive((Float) input, FLOAT_DATUM_WRITER);
    } else if (input instanceof ByteBuffer) {
      return serializePrimitive((ByteBuffer) input, BYTES_DATUM_WRITER);
    } else if (input instanceof byte[]) {
      return serializePrimitive(ByteBuffer.wrap((byte[]) input), BYTES_DATUM_WRITER);
    } else if (input instanceof Boolean) {
      return serializePrimitive((Boolean) input, BOOL_DATUM_WRITER);
    } else {
      throw new SamzaException(
          "Can only serialize avro objects, and primitives, cannot serialize: " + input.getClass().toString());
    }
  }

  /**
   * @param input primitive object to be serialized (Utf8, int, ...)
   * @param writer DatumWriter to use for the serialization
   * @param <T> type of the input
   * @return avro binary serialized byte[]
   */
  private static <T> byte[] serializePrimitive(T input, DatumWriter<T> writer) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BinaryEncoder encoder = AvroCompatibilityHelper.newBinaryEncoder(out);
    try {
      writer.write(input, encoder);
      encoder.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed to write input: " + input + " to binary encoder", e);
    }
    return out.toByteArray();
  }

  public void setExitMode(SamzaExitMode exitMode) {
    if (pushMonitor.isPresent()) {
      pushMonitor.get().setStreamReprocessingExitMode(exitMode);
    }
  }

  /**
   * Only used by tests
   */
  public String getKafkaBootstrapServers() {
    return this.kafkaBootstrapServers;
  }

  /**
   * For testing only.
   */
  public VeniceWriter<byte[], byte[], byte[]> getInternalProducer() {
    return this.veniceWriter;
  }

}
