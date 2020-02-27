SELECT DISTINCT T1.REFERENCE_ID as reference_id,
       T1.REQUIREMENT_TYPE as requirement_type,
       T1.PRIMARY_TEXT as text,
       T1.URL AS about,
       T1.REC_DATETIME
FROM RIDW.VW_REQUIREMENT T1
WHERE T1.PROJECT_ID = 19  AND
  T1.REQUIREMENT_TYPE NOT IN ( 'Test Dependency','Formal Use Case (Text)','Technical Design Requirement','Change Request','Actor','Use Case','Subtopic','Formal Use Case (Text)','User Story','Spec Proxy','Function Point','Process Inventory','Term','Use Case Diagram' ) AND
  (SUBSTR(T1.URL,43,3) = 'CA_' OR LENGTH(T1.URL) = 65 OR LENGTH(T1.URL) = 75) AND
  TRUNC(T1.REC_DATETIME) <= TO_DATE(:endDate,'mm/dd/yyyy')
AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_ID <> -1 AND T1.REQUIREMENT_ID IS NOT NULL)
ORDER BY T1.REQUIREMENT_TYPE asc,
         T1.REFERENCE_ID asc