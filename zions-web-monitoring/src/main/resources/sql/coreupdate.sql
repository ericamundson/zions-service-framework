SELECT T1.REFERENCE_ID as reference_id,
  T1.URL as about,
  T1.Primary_Text as text
FROM RIDW.VW_REQUIREMENT T1
LEFT OUTER JOIN RICALM.VW_RQRMENT_ENUMERATION T2
ON T2.REQUIREMENT_ID=T1.REQUIREMENT_ID AND T2.NAME='Release'
WHERE T1.PROJECT_ID = 19  AND
(  T1.REQUIREMENT_TYPE NOT IN ( 'Change Request','Actor','Use Case','Formal Use Case (Text)','Subtopic','User Story','Spec Proxy','Function Point','Process Inventory','Term','Use Case Diagram' ) AND
  T2.LITERAL_NAME = 'Deposits'  AND
  (SUBSTR(T1.URL,43,3) = 'CA_' OR LENGTH(T1.URL) = 65) AND
  TRUNC(T1.REC_DATETIME) >= TO_DATE(:endDate,'mm/dd/yyyy')
) AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_ID <> -1 AND T1.REQUIREMENT_ID IS NOT NULL)