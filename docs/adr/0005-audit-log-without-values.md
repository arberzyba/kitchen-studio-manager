# 5. Audit log records field names, not values

## Context

The GDPR asks for accountability: it should be possible to tell who changed personal data and when. A classic audit log stores the old and new value of every field. That would copy names, addresses and phone numbers into a second place, which then has to be protected and erased as well.

## Decision

Every insert, update and delete of a business record is logged with the user, the time, the kind of record, its id and the names of the changed fields, for example `Customer 12: lastName, email`. The values are not stored.

Entries are written by a Hibernate event listener, not by the services, and through plain JDBC inside the same transaction as the change.

## Why

- The log answers "who changed what, and when" without holding personal data, so erasing a customer does not require cleaning the log.
- A listener at the persistence layer cannot be forgotten when a new feature is added.
- Writing in the same transaction means an entry exists only if the change was committed.
- Hibernate's own session must not be used while it is flushing, hence JDBC for the insert.

## Consequences

- The log cannot show what a value used to be, so it cannot be used to undo a change.
- Reads are not logged, except the GDPR export of a customer's data.
- The user is stored as a plain id without a foreign key, so log entries survive whatever happens to the user record.
- Hibernate Envers would give full history with little code, but it stores values and so runs against the first point.
