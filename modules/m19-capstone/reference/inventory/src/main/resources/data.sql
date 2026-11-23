INSERT INTO toolshed_inventory.category (id, name) VALUES
  ('cat-drill', 'Drills and drivers'),
  ('cat-saw', 'Saws'),
  ('cat-sand', 'Sanders'),
  ('cat-garden', 'Garden'),
  ('cat-access', 'Ladders and access'),
  ('cat-measure', 'Measuring');

INSERT INTO toolshed_inventory.branch (id, name, address, opening_hours) VALUES
  ('br-north', 'Northside Library', '12 Elm Road', 'Tue-Thu 16:00-19:00, Sat 10:00-14:00'),
  ('br-river', 'Riverside Hub', '3 Quay Street', 'Mon-Fri 08:00-12:00'),
  ('br-market', 'Market Hall', '40 Market Square', 'Sat-Sun 09:00-17:00');

INSERT INTO toolshed_inventory.tool (id, barcode, name, condition, category_id, branch_id, added_at, kind, volts, cordless, weight_grams) VALUES
  ('t01', 'TS-0001', 'Cordless drill driver', 'GOOD', 'cat-drill', 'br-north', '2025-03-01T10:00:00Z', 'POWER', 18, true, NULL),
  ('t02', 'TS-0002', 'Hammer drill', 'WORN', 'cat-drill', 'br-north', '2025-03-01T10:05:00Z', 'POWER', 230, false, NULL),
  ('t03', 'TS-0003', 'Impact driver', 'NEW', 'cat-drill', 'br-river', '2026-06-14T09:00:00Z', 'POWER', 18, true, NULL),
  ('t04', 'TS-0004', 'Screwdriver set', 'GOOD', 'cat-drill', 'br-market', '2025-04-11T12:00:00Z', 'HAND', NULL, NULL, 900),
  ('t05', 'TS-0005', 'Circular saw', 'GOOD', 'cat-saw', 'br-north', '2025-05-02T15:30:00Z', 'POWER', 230, false, NULL),
  ('t06', 'TS-0006', 'Jigsaw', 'GOOD', 'cat-saw', 'br-river', '2025-05-20T11:00:00Z', 'POWER', 18, true, NULL),
  ('t07', 'TS-0007', 'Hand saw', 'WORN', 'cat-saw', 'br-market', '2024-11-30T10:00:00Z', 'HAND', NULL, NULL, 650),
  ('t08', 'TS-0008', 'Mitre saw', 'NEEDS_REPAIR', 'cat-saw', 'br-north', '2025-01-15T10:00:00Z', 'POWER', 230, false, NULL),
  ('t09', 'TS-0009', 'Coping saw', 'GOOD', 'cat-saw', 'br-river', '2025-09-09T09:09:00Z', 'HAND', NULL, NULL, 300),
  ('t10', 'TS-0010', 'Random orbit sander', 'GOOD', 'cat-sand', 'br-north', '2025-06-01T10:00:00Z', 'POWER', 230, false, NULL),
  ('t11', 'TS-0011', 'Detail sander', 'NEW', 'cat-sand', 'br-market', '2026-10-01T10:00:00Z', 'POWER', 12, true, NULL),
  ('t12', 'TS-0012', 'Sanding block', 'GOOD', 'cat-sand', 'br-river', '2025-02-02T10:00:00Z', 'HAND', NULL, NULL, 150),
  ('t13', 'TS-0013', 'Belt sander', 'WORN', 'cat-sand', 'br-north', '2024-08-08T10:00:00Z', 'POWER', 230, false, NULL),
  ('t14', 'TS-0014', 'Hedge trimmer', 'GOOD', 'cat-garden', 'br-market', '2025-04-04T10:00:00Z', 'POWER', 36, true, NULL),
  ('t15', 'TS-0015', 'Lawn mower', 'GOOD', 'cat-garden', 'br-market', '2025-04-05T10:00:00Z', 'POWER', 36, true, NULL),
  ('t16', 'TS-0016', 'Spade', 'GOOD', 'cat-garden', 'br-north', '2024-10-10T10:00:00Z', 'HAND', NULL, NULL, 2100),
  ('t17', 'TS-0017', 'Garden fork', 'WORN', 'cat-garden', 'br-river', '2024-10-10T10:05:00Z', 'HAND', NULL, NULL, 1900),
  ('t18', 'TS-0018', 'Pruning shears', 'NEW', 'cat-garden', 'br-market', '2026-09-20T10:00:00Z', 'HAND', NULL, NULL, 250),
  ('t19', 'TS-0019', 'Leaf blower', 'GOOD', 'cat-garden', 'br-river', '2025-10-30T10:00:00Z', 'POWER', 36, true, NULL),
  ('t20', 'TS-0020', 'Pressure washer', 'NEEDS_REPAIR', 'cat-garden', 'br-north', '2025-07-07T10:00:00Z', 'POWER', 230, false, NULL),
  ('t21', 'TS-0021', 'Step ladder', 'GOOD', 'cat-access', 'br-north', '2024-06-01T10:00:00Z', 'HAND', NULL, NULL, 7500),
  ('t22', 'TS-0022', 'Extension ladder', 'GOOD', 'cat-access', 'br-river', '2024-06-01T10:10:00Z', 'HAND', NULL, NULL, 14000),
  ('t23', 'TS-0023', 'Trestle pair', 'WORN', 'cat-access', 'br-market', '2025-01-20T10:00:00Z', 'HAND', NULL, NULL, 9000),
  ('t24', 'TS-0024', 'Laser level', 'NEW', 'cat-measure', 'br-north', '2026-11-01T10:00:00Z', 'POWER', 3, true, NULL),
  ('t25', 'TS-0025', 'Spirit level', 'GOOD', 'cat-measure', 'br-river', '2024-09-15T10:00:00Z', 'HAND', NULL, NULL, 700),
  ('t26', 'TS-0026', 'Tape measure', 'GOOD', 'cat-measure', 'br-market', '2024-09-15T10:05:00Z', 'HAND', NULL, NULL, 200),
  ('t27', 'TS-0027', 'Stud finder', 'GOOD', 'cat-measure', 'br-north', '2025-11-11T10:00:00Z', 'POWER', 9, true, NULL),
  ('t28', 'TS-0028', 'Moisture meter', 'NEW', 'cat-measure', 'br-river', '2026-08-08T10:00:00Z', 'POWER', 9, true, NULL),
  ('t29', 'TS-0029', 'Angle grinder', 'GOOD', 'cat-saw', 'br-market', '2025-12-12T10:00:00Z', 'POWER', 18, true, NULL),
  ('t30', 'TS-0030', 'Claw hammer', 'GOOD', 'cat-drill', 'br-river', '2024-05-05T10:00:00Z', 'HAND', NULL, NULL, 600);
