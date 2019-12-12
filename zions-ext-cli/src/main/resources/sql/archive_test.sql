SELECT DISTINCT T1.REFERENCE_ID,
       T1.REQUIREMENT_TYPE,
       T1.PRIMARY_TEXT,
       T1.URL AS URL1,
       T1.REC_DATETIME
FROM RIDW.VW_REQUIREMENT T1
WHERE T1.PROJECT_ID = 19  AND
(  CAST(CAST(CURRENT_TIMESTAMP  AS DATE) - CAST(T1.REC_DATETIME AS DATE) AS INTEGER) <= 20 AND
  TRUNC(T1.REC_DATETIME) <= TO_DATE(:endDate,'mm/dd/yyyy') AND
) AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_ID <> -1 AND T1.REQUIREMENT_ID IS NOT NULL)
ORDER BY T1.REQUIREMENT_TYPE asc,
         T1.REFERENCE_ID asc