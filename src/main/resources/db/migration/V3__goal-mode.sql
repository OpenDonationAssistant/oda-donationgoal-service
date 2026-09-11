alter table goal
  rename column isdefault to mode;

alter table goal
  alter column mode type varchar(255) using case
    when mode then 'DEFAULT'
    else 'CHOOSE'
  end;

alter table goal
  alter column mode set default 'CHOOSE';