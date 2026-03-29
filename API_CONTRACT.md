# API Contract

## Pagination Response Standard

All paginated endpoints must return this shared envelope shape:

- `content`: list of items for the requested page
- `totalElements`: total number of matching items
- `totalPages`: total number of pages
- `currentPage`: current page index (0-based)
- `size`: page size

### Backward Compatibility

To avoid breaking existing clients, the API also includes legacy pagination fields:

- `number`: alias of `currentPage`
- `first`: `true` when the current page is the first page
- `last`: `true` when the current page is the last page

`currentPage` and `totalPages` are the canonical fields for new client code.
Legacy fields remain in v1 responses for backward compatibility.

## Endpoint: Search Products

- Method: `GET`
- Path: `/api/products/search`

Query params:

- `name` (optional, string)
- `minPrice` (optional, number)
- `maxPrice` (optional, number)
- `categoryId` (optional, number)
- `page` (optional, integer, default `0`)
- `size` (optional, integer, default `10`)
- `sort` (optional, string, default `id,asc`)

### Response 200

```json
{
  "content": [
    {
      "id": 1,
      "name": "Red Rose Bouquet",
      "price": 250000,
      "description": "Fresh red roses",
      "image": "https://example.com/rose.jpg",
      "categoryId": 2,
      "categoryName": "Roses"
    }
  ],
  "totalElements": 27,
  "totalPages": 3,
  "currentPage": 1,
  "size": 10,
  "number": 1,
  "first": false,
  "last": false
}
```
