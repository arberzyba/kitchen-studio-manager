# 6. GDPR erasure deletes or anonymizes

## Context

A customer can ask for their personal data to be erased (GDPR Art. 17). At the same time, German commercial and tax law requires a company to keep business documents such as invoices for several years. The GDPR allows keeping data that a legal obligation requires (Art. 17(3)(b)).

## Decision

Erasure is one action for the admin with two outcomes:

- **No quotes, orders or invoices:** the customer and their contact history are deleted.
- **Business documents exist:** the customer is anonymized. Name, company, email, phone, both addresses, the contact history and the free-text notes on their quotes are removed, and the record is marked as anonymized. Quotes, orders and invoices stay.

An issued invoice keeps the recipient name and address it was issued with, because that copy is part of the document that has to be retained.

An anonymized customer no longer appears in search and cannot be edited, contacted or quoted again. The erasure is recorded in the audit log.

The matching export (Art. 15 and 20) returns everything stored about a customer as one JSON file and is also recorded.

## Why

- Deleting a customer with invoices would either fail on database constraints or destroy documents the company must keep.
- Refusing erasure altogether would ignore the customer's right.
- Anonymizing removes what is not needed and keeps what the law requires.

## Consequences

- After anonymization, the customer's old name is still readable on their invoices until the retention period ends. Deleting those documents after that period is not automated.
- Free text elsewhere, such as appointment notes, is not scrubbed.
- This is a technical implementation of a common approach, not legal advice.
