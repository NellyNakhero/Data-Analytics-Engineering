# Directed Acrylic Graph (DAG)

- It represents a collection of tasks and their dependencies, dictating the order in which they should be executed.
- It provides a blueprint for executing a series of interconnected tasks in a controlled and organised manner, enabling robust data pipelines and automated processes.

### Directed:

- This means their is a clear flow of execution, with tasks progressing in a specific direction from one to the next. There are no loops or backward tasks.

### Acrylic

- This signifies that there are no cycles or circular dependencies within the graph.
- A task cannot depend on itself, either directly or indirectly through a chain of other tasks, ensuring a definite start and endpoint for the workflow.


### Graph

- This refers to the visual representation of tasks as nodes and their dependencies as directed edges, illustrating the workflow's structure.


## Key Aspects Of An Airflow DAG

#### Tasks:

- These are individual units of work within a workflow, defined by operators.


#### Dependencies:

- DAGs define the relationships between tasks, specifying which task must complete successfully before others can begin.
- This is achieved using bitshift operators(e.g `task_a a >> task_b`) or `set_upstream`/ `set_downstream` methods.

#### Scheduling

- DAGs include parameters like `start_date`, `end_date` and `schedule` to determine when and how often a workflow should run.

#### Python Definition

- Airflow DAGs are defined in python scripts, which are essentially configuration files that layout the structure of the workflow in code.
- This script is processed by the airflow scheduler to create and manage DAG runs.