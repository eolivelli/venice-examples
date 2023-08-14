scenario.run({
    'driver':     'venice',
    'workload':   'venice_reader.yaml',
    'store_name': 'store1',
    'router_url': 'http://localhost:7777',
    'cycles':     '100'
});
