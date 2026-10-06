# 3. Documents keep their own copy of the data

## Context

Quotes, orders and invoices refer to products and customers. Products change price and name over time, and customers move. A quote sent last month must still say what it said last month, and an invoice is a legal document that must not change after it is issued.

## Decision

Documents copy what they print at the moment they are created:

- A quote line copies the product's article number, name, unit and price.
- A supplier order line copies the product's purchase price.
- An invoice copies the recipient's name and address and the net, VAT and gross amounts, and stores the VAT rate.

The link to the product or customer is kept as well, for navigation and reporting.

A quote can be edited only while it is a draft. An order takes its items from its accepted quote, which can no longer change, so those are not copied a second time.

## Why

- Changing the catalog or a customer record never alters an existing document. Tests check this for quotes and invoices.
- Totals are stored on the quote and invoice, so lists and the dashboard do not recalculate them.
- Storing the VAT rate with the document means a future change of the legal rate does not rewrite history.

## Consequences

- Some data is stored twice on purpose.
- Correcting an issued invoice would need a cancellation invoice, which is not implemented.
