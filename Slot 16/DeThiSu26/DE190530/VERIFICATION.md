# K?t qu? ki?m th? Hotel

- Th?i ?i?m UTC: `2026-10-28T13:40:00.594991+00:00`.
- M?i tr??ng: Windows 11, Oracle JDK 21.0.11, Maven 3.9.12, SQL Server 2022 Developer, database `MSS301_2026_PE`.
- Build: `mvn clean package` th?nh c?ng cho c? ba module.
- Ki?m th?: `python scripts/verify.py` ? **71/71 ??t**, qua gateway ? MVC ? OpenFeign ? SQL Server th?t.
- Prefix d? li?u l?n ch?y: `e2e_2bc5bd0037`.
- ?ng d?ng do script kh?i ??ng ?? ???c d?ng sau ki?m th?. Database v? d? li?u m?u/ki?m th? ???c gi? l?i theo y?u c?u.
- SQL Server 2016 ch?a ???c ch?y th?c t?; kh?ng c? ki?m th? GUI tr?nh duy?t. Swagger ???c ki?m tra qua HTML, OpenAPI JSON v? swagger-config HTTP.

## C?c ki?m tra ??t

1. create master
2. duplicate master
3. create second master
4. duplicate update
5. duplicate update leaves data intact
6. master required fields
7. master invalid 1
8. master invalid 2
9. master invalid 3
10. master invalid 4
11. master invalid 5
12. master invalid 6
13. master invalid 7
14. master invalid 8
15. master invalid 9
16. master partial update
17. master retained omitted fields
18. master negative page
19. master size too large
20. master zero size
21. master invalid filter
22. master invalid kind
23. master paginated filters
24. missing master GET
25. missing master PUT
26. missing master DELETE
27. invalid path ID
28. malformed JSON
29. create child via real feign
30. nested master detail
31. child validation 0
32. child validation 1
33. child validation 2
34. child validation 3
35. child validation 4
36. child validation 5
37. child validation 6
38. child validation 7
39. child validation 8
40. child validation 9
41. child validation 10
42. feign 404 mapping
43. reject invalid partial dates
44. invalid patch did not persist
45. reject partial count above capacity
46. update current price
47. partial child update recalculates price
48. child name and status filter
49. child invalid query size=101
50. child invalid query page=-1
51. child invalid query size=0
52. child invalid query status=INVALID
53. child invalid query size=abc
54. missing child GET
55. missing child PUT
56. missing child DELETE
57. cancel child
58. cancel is soft delete
59. deactivate master
60. master soft delete persists
61. inactive source rejected
62. update existing booking after deactivation
63. room swagger through gateway
64. reservation swagger through gateway
65. gateway swagger definitions
66. public swagger UI
67. gateway CORS preflight
68. single CORS origin header 8080
69. single CORS origin header 8081
70. single CORS origin header 8082
71. source outage returns server error
