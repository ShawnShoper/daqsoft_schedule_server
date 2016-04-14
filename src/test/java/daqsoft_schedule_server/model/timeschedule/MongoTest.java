package daqsoft_schedule_server.model.timeschedule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.daqsoft.schedule.pojo.TaskStatus;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.ServerAddress;

public class MongoTest
{
	MongoTemplate mongoClient = null;
	@Before
	public void init1() throws Exception
	{
		Builder mongoClientOptions = MongoClientOptions.builder();
		mongoClientOptions.maxWaitTime(30000);
		mongoClientOptions.connectTimeout(30000);
		List<ServerAddress> serverAddresses = new ArrayList<ServerAddress>();
		serverAddresses.add(new ServerAddress("192.168.0.82", 27017));
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(
				new MongoClient(serverAddresses, mongoClientOptions.build()),
				"daq");
		MappingMongoConverter converter = new MappingMongoConverter(
				mongoDbFactory, new MongoMappingContext());
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		// Instancing a spring mongoTemplate
		mongoClient = new MongoTemplate(mongoDbFactory, converter);
	}
	@Test
	public void add_test()
	{
		TaskStatus ts = new TaskStatus();
		ts.setToken(UUID.randomUUID().toString());
		ts.setStatus(0);
		ts.setTaskName("aasd");

		mongoClient.save(ts);
	}
	@Test
	public void findOne() throws IOException
	{
		Query.query(Criteria.where("_id").is("568f4d337d84797d575dc099"));
		DBObject query = new BasicDBObject();
		query.put("_id", new ObjectId("568f4d337d84797d575dc099"));
		DBCursor dbCursor = mongoClient.getCollection("taskTemplate")
				.find(query);
		FileOutputStream fis = new FileOutputStream(
				new File("/Users/ShawnShoper/Desktop/baidu.java"));
		while (dbCursor.hasNext())
		{
			DBObject data = dbCursor.next();
			String code = String.valueOf(data.get("code"));
			fis.write(code.getBytes());
			System.out.println(code);
		}
		fis.flush();
		fis.close();
		System.out.println("..");
	}
	@Test
	public void remove_test()
	{
		mongoClient.remove(
				Query.query(Criteria.where("_id")
						.is("f66d2c57-71a4-4404-a3c4-17e7b72cd794")),
				TaskStatus.class);
	}
}
