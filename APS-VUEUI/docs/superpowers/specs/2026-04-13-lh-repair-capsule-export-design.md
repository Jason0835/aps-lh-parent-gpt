# lhRepairCapsule Export Design

## Summary
Add an export button to the LH Repair Capsule list page. The button is permission-gated and exports all data matching the current filters (no pagination), following existing LH module patterns.

## Scope
- Frontend-only change in `lhRepairCapsule/index.vue`
- Add export button in header
- Add export handler and API call via `downloadLink`
- Add permission gate `lh:lhRepairCapsule:export`

## UI And Permissions
- Place an "Export" button in the `page-table` header slot
- Button text uses `ui.frame.btn.export`
- Permission: `v-hasPermi="['lh:lhRepairCapsule:export']"`

## Data Flow
- User sets search filters
- `handleSearch` maps `obtainTime` to `obtainTimeBegin/obtainTimeEnd`
- Export uses `formatParams(false)` to avoid paging params
- Trigger `downloadLink('/lh/lhRepairCapsule/export', params)`

## API Contract
- Endpoint: `POST /lh/lhRepairCapsule/export`
- Request body mirrors list filters and sort fields
- Response: file download handled by `downloadLink`

## Error Handling
- Rely on existing `downloadLink` error handling behavior
- No additional UI state or error messaging added

## Testing
- Open page, set filters (including `obtainTime`), click Export
- Verify downloaded file matches current filters and includes all records

## Out Of Scope
- Backend changes or new permissions provisioning
- New columns, search fields, or UI refactors
