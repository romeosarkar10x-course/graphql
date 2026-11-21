# Hints: m11/05

1. Two changes: something that listens to the table and publishes, and `sendMessage` no longer publishing
   itself (or every API message arrives twice). The listener is a `DebeziumEngine` started by a Spring
   bean, running on its own thread.
2. `DebeziumEngine.create(Json.class).using(properties).notifying(event -> ...).build()`, run on an
   executor. The properties name the connector (`io.debezium.connector.postgresql.PostgresConnector`),
   the database, `plugin.name=pgoutput`, `table.include.list`, a `slot.name`, `topic.prefix`, and an
   offset store (`org.apache.kafka.connect.storage.MemoryOffsetBackingStore` is fine here).
3. With `value.converter.schemas.enable=false` each event's value is JSON with `op` (`c` for an insert)
   and `after` (the new row: `id`, `room_id`, `author`, `body`, `sent_at`). `snapshot.mode=no_data` skips
   the rows already there. Wait for the engine's `ConnectorCallback.taskStarted()` before the server
   counts as started, or the first inserts are missed.
