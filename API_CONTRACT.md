# API_CONTRACT

## Base URL

`http://localhost:8080`

## Authentication

Cart APIs require `Authorization: Bearer <JWT>`.

- Backend reads `sub` claim from JWT payload and uses it as `userId`.
- Signature validation is not implemented in this phase.

## Product APIs

### GET `/api/products/search`

Search products with filters and pagination.

Query params:

- `name` (optional, string)
- `minPrice` (optional, number)
- `maxPrice` (optional, number)
- `categoryId` (optional, number)
- `page` (optional, default `0`)
- `size` (optional, default `10`)
- `sort` (optional, default `id,asc`)

Response `200`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Bo hong do co dien",
      "price": 350000,
      "description": "Bo 20 bong hong do tuoi",
      "imageUrl": "hong-do-co-dien.jpg",
      "stockQuantity": 28,
      "categoryName": "Hoa hong"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 20,
  "totalPages": 2,
  "last": false
}
```

### GET `/api/products/{id}`

Get product detail.

Path params:

- `id` (required, number)

Response `200`:

```json
{
  "id": 1,
  "name": "Bo hong do co dien",
  "price": 350000,
  "description": "Bo 20 bong hong do tuoi",
  "image": "hong-do-co-dien.jpg",
  "stockQuantity": 28,
  "createdAt": "2026-03-30T21:00:00",
  "updatedAt": "2026-03-30T21:00:00",
  "categoryId": 1,
  "categoryName": "Hoa hong"
}
```

Response `404`:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/products/999999"
}
```

## Cart APIs

### POST `/api/cart/items`

Add product into cart.

Request body:

```json
{
  "productId": 1,
  "quantity": 2
}
```

Response `200`:

```json
{
  "cartId": 1,
  "userId": "user-123",
  "totalItems": 2,
  "totalAmount": 700000,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Bo hong do co dien",
      "image": "hong-do-co-dien.jpg",
      "quantity": 2,
      "price": 350000,
      "lineTotal": 700000,
      "availableStock": 28
    }
  ],
  "updatedAt": "2026-03-30T21:05:00"
}
```

Response `400` (validation or stock):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient stock for product id 1",
  "path": "/api/cart/items"
}
```

### GET `/api/cart`

Get current user cart.

Response `200` (empty cart):

```json
{
  "cartId": null,
  "userId": "user-123",
  "totalItems": 0,
  "totalAmount": 0,
  "items": [],
  "updatedAt": null
}
```

### PUT `/api/cart/items/{id}`

Update item quantity.

Path params:

- `id` (required, cart item id)

Request body:

```json
{
  "quantity": 3
}
```

Response `200`: same shape as cart response.

### DELETE `/api/cart/items/{id}`

Delete item from cart.

Path params:

- `id` (required, cart item id)

Response `204` with empty body.

### Error codes summary

- `400`: invalid quantity, insufficient stock
- `401`: missing/invalid bearer token or missing `sub`
- `404`: product/cart/cart item not found

## Admin Product APIs (VUO-69, VUO-70)

These endpoints require admin role header:

- `X-Role: ADMIN`

### POST `/api/admin/products`

Create product.

### PUT `/api/admin/products/{id}`

Update product.

### DELETE `/api/admin/products/{id}`

Soft delete product (sets `deletedAt`).

### PATCH `/api/admin/products/{id}/restore`

Restore soft-deleted product (`deletedAt = null`).

### PATCH `/api/admin/products/{id}/stock`

Update stock quantity.

## Order and COD APIs (VUO-78)

### POST `/api/orders`

Create order with `paymentMethod` (`COD`, `VIETQR`, `SEPAY`).

### GET `/api/orders/{id}`

Get order detail.

### POST `/api/orders/{id}/cod/confirm`

Confirm COD order. Rule:

- only `paymentMethod = COD`
- only `status = PENDING`

## Payment APIs (VUO-77, VUO-80, VUO-82)

### POST `/api/payments/vietqr/orders/{orderId}/checkout`

Generate VietQR checkout payload and QR content.

### POST `/api/payments/sepay/orders/{orderId}/checkout`

Generate SePay payment link.

### POST `/api/payments/vietqr/webhook`

Webhook process with idempotency and validation:

- signature (when configured)
- amount equals order amount
- duplicate transaction detection by `(paymentMethod, providerTransactionId)`
- retry-safe order update: only update when `order.status = PENDING`

### POST `/api/payments/sepay/webhook`

Same validations as VietQR plus secret validation (`X-Sepay-Secret` or body secret).

### GET `/api/payments/orders/{orderId}/reconcile`

Payment reconciliation summary for the order.

## Media Storage APIs (VUO-74)

These endpoints require `X-Role: ADMIN`.

### POST `/api/admin/upload`

Upload image (`multipart/form-data`, field `file`).

Rules:

- allowed types: `image/jpeg`, `image/png`, `image/webp`
- max size: 5MB

### DELETE `/api/admin/upload/{key}`

Delete object by key.

### GET `/api/admin/upload/{key}/signed-url`

Generate signed URL for read access.
