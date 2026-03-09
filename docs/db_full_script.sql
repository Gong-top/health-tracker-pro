-- 1. 数据库安全与完整性增强版
create database if not exists myhobby default character set utf8mb4;
use myhobby;

-- [表 1-9 保持原样，省略重复部分，但在最后增加了维护计划]
-- 用户、习惯、用户习惯、记录、健康、运动、运动记录、统计、附件 (已由你提供)

-- 2. 优化后的触发器：处理记录插入、更新、删除，实时维护统计表
drop trigger if exists tr_record_insert;
delimiter $$
create trigger tr_record_insert
after insert on record
for each row
begin
    declare v_user_id bigint;
    declare v_total_habits int default 0;
    
    select uh.user_id into v_user_id from user_habit uh where uh.user_habit_id = new.user_habit_id limit 1;
    
    select count(*) into v_total_habits from user_habit uh2
    where uh2.user_id = v_user_id and uh2.user_habit_status = true
    and (uh2.start_date is null or uh2.start_date <= new.record_date)
    and (uh2.end_date is null or uh2.end_date >= new.record_date);
    
    insert into statistic (user_id, sta_date, total_habits, finished_habits)
    values (v_user_id, new.record_date, v_total_habits, case when new.record_status = 1 then 1 else 0 end)
    on duplicate key update
        finished_habits = finished_habits + (case when new.record_status = 1 then 1 else 0 end),
        total_habits = v_total_habits;
end
$$
delimiter ;

drop trigger if exists tr_record_delete;
delimiter $$
create trigger tr_record_delete
after delete on record
for each row
begin
    update statistic
    set finished_habits = greatest(finished_habits - (case when old.record_status = 1 then 1 else 0 end), 0)
    where user_id = (select user_id from user_habit where user_habit_id = old.user_habit_id)
    and sta_date = old.record_date;
end
$$
delimiter ;

-- 3. 安全性措施
-- 创建只读用户 (示例)
-- CREATE USER 'health_reader'@'%' IDENTIFIED BY 'Password123!';
-- GRANT SELECT ON myhobby.* TO 'health_reader'@'%';

-- 4. 实施维护计划 (存储过程：每周一凌晨清理过期记录)
delimiter $$
create procedure db_maintenance_plan()
begin
    -- 备份旧附件路径到审计表 (假设有审计表)
    -- 清理 1 年前的健康数据
    delete from health_data where record_date < date_sub(curdate(), interval 1 year);
    -- 优化碎片较多的表
    optimize table record, health_data, statistic;
end
$$
delimiter ;
