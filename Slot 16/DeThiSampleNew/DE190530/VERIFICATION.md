# K?t qu? ki?m th? Media

- Th?i ?i?m UTC: `2026-10-28T13:39:14.106295+00:00`.
- M?i tr??ng: Windows 11, Oracle JDK 21.0.11, Maven 3.9.12, SQL Server 2022 Developer, database `MSS301_2026_PE_MEDIA`.
- Build: `mvn clean package` th?nh c?ng cho c? ba module.
- Ki?m th?: `python scripts/verify.py` ? **73/73 ??t**, qua gateway ? MVC ? OpenFeign ? SQL Server th?t.
- Prefix d? li?u l?n ch?y: `e2e_a704324bd2`.
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
16. master invalid 10
17. master invalid 11
18. master partial update
19. master retained omitted fields
20. master negative page
21. master size too large
22. master zero size
23. master invalid filter
24. master invalid kind
25. master paginated filters
26. missing master GET
27. missing master PUT
28. missing master DELETE
29. invalid path ID
30. malformed JSON
31. create child via real feign
32. nested master detail
33. child validation 0
34. child validation 1
35. child validation 2
36. child validation 3
37. child validation 4
38. child validation 5
39. child validation 6
40. child validation 7
41. child validation 8
42. child validation 9
43. child validation 10
44. feign 404 mapping
45. reject invalid partial dates
46. invalid patch did not persist
47. reject partial count above capacity
48. update current price
49. partial child update recalculates price
50. child name and status filter
51. child invalid query size=101
52. child invalid query page=-1
53. child invalid query size=0
54. child invalid query status=INVALID
55. child invalid query size=abc
56. missing child GET
57. missing child PUT
58. missing child DELETE
59. cancel child
60. cancel is soft delete
61. deactivate master
62. master soft delete persists
63. inactive source rejected
64. update existing booking after deactivation
65. user swagger through gateway
66. post swagger through gateway
67. gateway swagger definitions
68. public swagger UI
69. gateway CORS preflight
70. single CORS origin header 8080
71. single CORS origin header 8081
72. single CORS origin header 8082
73. source outage returns server error
