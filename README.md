# slack2affiliate

local run

`sbt run`

deploy

```
sbt clean dist
cd ansible
ansible-playbook -i inventory/hosts site.yml
```
