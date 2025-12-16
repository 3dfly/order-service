# Quick Reference - curl Commands

Copy and paste these commands to test the API. Replace `test_cube.stl` with your own STL file path.

## Valid Combinations

### FDM + PLA + Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### FDM + ABS + No Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=ABS" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=false" | jq
```

### FDM + PETG + Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=3" \
  -F "infill=20" \
  -F "supporters=true" | jq
```

### FDM + TPU + No Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=TPU" \
  -F "layerHeight=0.3" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=false" | jq
```

### SLS + PLA + Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### SLS + ABS + No Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=ABS" \
  -F "layerHeight=0.15" \
  -F "shells=3" \
  -F "infill=20" \
  -F "supporters=false" | jq
```

### SLS + PETG + Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=10" \
  -F "supporters=true" | jq
```

### SLA + PLA + Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=PLA" \
  -F "layerHeight=0.1" \
  -F "shells=4" \
  -F "infill=10" \
  -F "supporters=true" | jq
```

### SLA + ABS + No Supporters
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=ABS" \
  -F "layerHeight=0.15" \
  -F "shells=3" \
  -F "infill=15" \
  -F "supporters=false" | jq
```

---

## Invalid Combinations (Should return 400 error)

### SLS + TPU (Invalid)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=TPU" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### SLA + PETG (Invalid)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### SLA + TPU (Invalid)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=TPU" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

---

## Validation Errors (Should return 400 error)

### Missing Supporters Parameter
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" | jq
```

### Invalid Layer Height (Too Small)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.01" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### Invalid Layer Height (Too Large)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.5" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### Invalid Shells Count (Too Many)
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=10" \
  -F "infill=15" \
  -F "supporters=true" | jq
```

### Invalid Infill Value
```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=25" \
  -F "supporters=true" | jq
```

---

## Notes

- Add `| jq` at the end for pretty JSON formatting (requires jq installed)
- Remove `| jq` if you don't have jq installed
- Replace `test_cube.stl` with the path to your STL file
- The API must be running on http://localhost:8080
