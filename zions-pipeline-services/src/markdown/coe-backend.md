# Introduction

This document will detail the development processes and backend processing to work 
with all DevOps as code via internal yaml, ADO yaml, and XL yaml.

### Main Development Flow

![Main Activities](Pipeline_main_activities.svg)

<div hidden>
```{r, include=FALSE}
@startuml Pipeline_main_activities.svg
start
if (doesn't have required blueprints) then (yes)
  :Create/update required blueprints;
endif
:Execute blueprints;

stop
@enduml
```
</div>