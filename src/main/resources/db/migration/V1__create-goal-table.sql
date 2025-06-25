create table goal (
  id varchar(255),
  recipient_id varchar(255),
  full_description text,
  brief_description text,
  widget_id varchar(255),
  accumulated_amount varchar(255),
  required_amount varchar(255),
  isDefault boolean default false,
  enabled boolean default true
);
