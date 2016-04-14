package com.daqsoft.schedule.server.dao;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.shoper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.daqsoft.schedule.module.MongoModule;
import com.daqsoft.schedule.pojo.Task;
import com.daqsoft.schedule.pojo.TaskStatus;
import com.daqsoft.schedule.pojo.TaskTemplate;
import com.daqsoft.schedule.server.pojo.NotifyTask;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Repository
public class TaskDao
{
	@Autowired
	private MongoModule mongo;

	/**
	 * 获取指定时间段之前的非定时任务
	 * 
	 * @param interval
	 *            时间区间
	 * @param unit
	 *            时间单位
	 * @param isTiming
	 *            是否定时,null 无条件，true 定时，false 非定时
	 * @param isDisabled是否禁用,null
	 *            无条件， true 禁用，false 非禁用
	 * @return 非定时任务
	 */
	public List<Task> getTask(int interval, TimeUnit unit, Boolean isTiming)
	{
		long diff = unit.toMillis(interval);
		Date condition = new Date(System.currentTimeMillis() - diff);
		Criteria criteria = Criteria.where("lastFinishTime").lte(condition)
				.and("disabled").is(false);
		if (isTiming != null)
		{
			criteria.and("timing").is(isTiming);
		}
		Query query = Query.query(criteria);
		List<Task> tasks = mongo.getMongoTemplate().find(query, Task.class);
		return tasks;
	}

	/**
	 * 获取定时任务..所有的..
	 * 
	 * @return 定时任务集合
	 */
	public List<Task> getTimingTask()
	{
		List<Task> tasks = mongo.getMongoTemplate().find(Query.query(
				Criteria.where("disabled").is(false).and("timing").is(true)),
				Task.class);
		return tasks;
	}

	public TaskTemplate getTaskTemplate(String templateID)
	{
		return mongo.getMongoTemplate().findOne(
				Query.query(Criteria.where("_id").is(templateID)),
				TaskTemplate.class);
	}

	/**
	 * 获取 所有task 分页
	 * 
	 * @param page
	 *            页码数
	 * @param row
	 *            每页显示
	 * @return
	 */
	public List<Task> getTask(int offset, int limit, String search, String sort,
			String order)
	{
		Query query = new Query();
		if (!StringUtil.isEmpty(sort) && !StringUtil.isEmpty(order))
			query.with(new Sort(Direction.fromString(order), sort));
		if (search != null)
			query.addCriteria(
					Criteria.where("name").regex(".*?" + search + ".*"));
		query.skip(offset);
		query.limit(limit);
		return mongo.getMongoTemplate().find(query, Task.class);
	}

	/**
	 * 获取 所有task template 分页
	 * 
	 * @param page
	 *            页码数
	 * @param row
	 *            每页显示
	 * @return
	 */
	public List<TaskTemplate> getTaskTemplate(int offset, int limit,
			String search, String sort, String order)
	{
		DBObject fieldObject = new BasicDBObject();
		fieldObject.put("code", false);
		Criteria criteria = new Criteria();
		Query q = new BasicQuery(new BasicDBObject(), fieldObject);
		if (!StringUtil.isEmpty(search))
			criteria.and("name").regex(".*?" + search + ".*");
		if (sort != null && order != null)
			q.with(new Sort(Direction.fromString(order), sort));
		q.skip(offset).limit(limit);
		q.addCriteria(criteria);

		return mongo.getMongoTemplate().find(q, TaskTemplate.class);
	}

	public long getTaskSize(String search)
	{
		Criteria criteria = new Criteria();
		// .where("disabled").is(false);
		if (search != null)
			criteria.and("name").regex(".*?" + search + ".*");
		Query query = Query.query(criteria);
		return mongo.getMongoTemplate().count(query, Task.class);
	}

	public long getTaskTemplateSize(String search)
	{
		Criteria criteria = new Criteria();// .where("disabled").is(false);
		if (search != null)
			criteria.and("name").regex(".*?" + search + ".*");
		Query query = Query.query(criteria);
		return mongo.getMongoTemplate().count(query, TaskTemplate.class);
	}
	/**
	 * 翻转状态..
	 * 
	 * @param id
	 *            id
	 * @param value
	 *            值
	 * @param type
	 *            类型
	 * @return
	 */
	public void inverseStatus(String id, boolean value, int type)
	{
		Query query = Query.query(Criteria.where("_id").is(id));
		Update update = new Update();
		update.set("disabled", !value);
		mongo.getMongoTemplate().updateFirst(query, update,
				type == 0 ? TaskTemplate.class : Task.class);
	}

	public void addTaskTemplate(TaskTemplate taskTemplate)
	{
		mongo.getMongoTemplate().save(taskTemplate);
	}

	public boolean taskTemplateHasExists(String name)
	{
		return mongo.getMongoTemplate().exists(
				Query.query(Criteria.where("name").is(name)),
				TaskTemplate.class);
	}
	public boolean taskTemplateHasExistsByID(String templateID)
	{
		return mongo.getMongoTemplate().exists(
				Query.query(Criteria.where("_id").is(templateID)),
				TaskTemplate.class);
	}
	public void pushNotifyTask(NotifyTask notifyTask)
	{
		Update update = new Update();
		update.set("domain", notifyTask.getDomain());
		update.set("type", notifyTask.getType());
		update.set("notify", false);
		mongo.getMongoTemplate()
				.upsert(Query.query(
						Criteria.where("domain").is(notifyTask.getDomain())),
						update, NotifyTask.class);
	}

	public NotifyTask notifyTaskInfo(String domain)
	{
		if (mongo.getMongoTemplate().exists(
				Query.query(Criteria.where("domain").is(domain)),
				NotifyTask.class))
		{
			return mongo.getMongoTemplate()
					.find(Query.query(Criteria.where("domain").is(domain)),
							NotifyTask.class)
					.get(0);
		}
		return null;
	}

	public void taskDone(String job, long endTime, AtomicLong saveCount)
	{
		Update update = new Update();
		update.inc("loops", 1);
		update.set("lastFinishTime", new Date(endTime));
		mongo.getMongoTemplate().updateFirst(
				Query.query(Criteria.where("_id").is(job)), update, Task.class);
	}

	public Task getTaskByID(String job)
	{
		return mongo.getMongoTemplate().findOne(
				Query.query(Criteria.where("_id").is(job)), Task.class);
	}

	public void taskFailed(String job)
	{
		Update update = new Update();
		update.inc("failedCount", 1);
		mongo.getMongoTemplate().updateFirst(
				Query.query(Criteria.where("_id").is(job)), update, Task.class);
	}

	public void disabledTask(String job)
	{
		Update update = new Update();
		update.set("disabled", true);
		mongo.getMongoTemplate().updateFirst(
				Query.query(Criteria.where("_id").is(job)), update, Task.class);
	}

	public void addTask(Task task)
	{
		mongo.getMongoTemplate().save(task);
	}

	public boolean taskHasExistsByID(String id)
	{
		return mongo.getMongoTemplate()
				.exists(Query.query(Criteria.where("_id").is(id)), Task.class);
	}

	public void deleteTask(String id)
	{
		mongo.getMongoTemplate()
				.remove(Query.query(Criteria.where("_id").is(id)), Task.class);
	}

	public void deleteTaskTemp(String id)
	{
		mongo.getMongoTemplate().remove(
				Query.query(Criteria.where("_id").is(id)), TaskTemplate.class);
	}

	public String getTaskGroup(String id)
	{
		return mongo.getMongoTemplate().findById(id, TaskTemplate.class)
				.getGroup();
	}

	public void editTask(Task task)
	{
		mongo.getMongoTemplate().save(task);
	}

	public boolean updateTaskStatus(String token)
	{
		TaskStatus taskStatus = mongo.getMongoTemplate().findById(toString(),
				TaskStatus.class);
		if (!StringUtil.isNull(taskStatus))
			mongo.getMongoTemplate().remove(
					Query.query(Criteria.where("_id").is(token)),
					TaskStatus.class);
		return false;
	}

}
