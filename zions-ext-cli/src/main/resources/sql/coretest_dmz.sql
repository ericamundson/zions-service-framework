select reference_id, about, text from (
WITH filtered AS (
SELECT DISTINCT T2.REFERENCE_ID AS REFERENCE_ID1,
       T2.URL AS URL1,
       T2.PRIMARY_TEXT AS REQTYPE1,
       T4.REFERENCE_ID AS REFERENCE_ID2,
       T4.URL AS URL2,
       T4.PRIMARY_TEXT AS REQTYPE2,
       T5.REFERENCE_ID AS REFERENCE_ID3,
       T5.URL AS URL3,
       T5.PRIMARY_TEXT AS REQTYPE3
FROM RIDW.VW_REQUIREMENT_COLLECTION T1
INNER JOIN RIDW.VW_REQUICOL_REQUIREMENT_LOOKUP LT1
ON (T1.REQUIREMENT_COLLECTION_ID = LT1.REQUIREMENT_COLLECTION_ID)
  INNER JOIN RIDW.VW_REQUIREMENT T2
  ON (T2.REQUIREMENT_ID = LT1.REQUIREMENT_ID) AND T2.PROJECT_ID = 19  AND T2.ISSOFTDELETED = 0
--    INNER JOIN RIDW.VW_REQUIREMENT_HIERARCHY LT2
--    ON (T2.REQUIREMENT_ID = LT2.PRED_REQUIREMENT_ID)
      INNER JOIN RIDW.VW_REQUIREMENT T3
      ON (T2.REFERENCE_ID = T3.REFERENCE_ID) AND T3.PROJECT_ID = 19  AND T3.ISSOFTDELETED = 0
        INNER JOIN RIDW.VW_REQUIREMENT_HIERARCHY LT3
        ON (T3.REQUIREMENT_ID = LT3.PRED_REQUIREMENT_ID)
          INNER JOIN RIDW.VW_REQUIREMENT T4
          ON (LT3.SUCC_REQUIREMENT_ID = T4.REQUIREMENT_ID) AND T4.PROJECT_ID = 19  AND T4.ISSOFTDELETED = 0
            LEFT OUTER JOIN RIDW.VW_REQUIREMENT_HIERARCHY LT4
            ON (T4.REQUIREMENT_ID = LT4.PRED_REQUIREMENT_ID)
              LEFT OUTER JOIN RIDW.VW_REQUIREMENT T5
              ON (LT4.SUCC_REQUIREMENT_ID = T5.REQUIREMENT_ID) AND T5.PROJECT_ID = 19  AND T5.ISSOFTDELETED = 0
WHERE T1.PROJECT_ID = 19 
 AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_COLLECTION_ID <> -1 AND T1.REQUIREMENT_COLLECTION_ID IS NOT NULL) AND
(T2.REQUIREMENT_ID <> -1 AND T2.REQUIREMENT_ID IS NOT NULL) AND
(T3.REQUIREMENT_ID <> -1 AND T3.REQUIREMENT_ID IS NOT NULL) AND
(T4.REQUIREMENT_ID <> -1 AND T4.REQUIREMENT_ID IS NOT NULL)
AND T1.REFERENCE_ID = '459375'
AND TRUNC(T1.REC_DATETIME) <= TO_DATE(:endDate,'mm/dd/yyyy')
)
select REFERENCE_ID1 as reference_id, url1 as about, REQTYPE1 as text from filtered
union
select REFERENCE_ID2, url2, REQTYPE2 from filtered
union
select REFERENCE_ID3, url3, REQTYPE3 from filtered where REFERENCE_ID3 is not null )