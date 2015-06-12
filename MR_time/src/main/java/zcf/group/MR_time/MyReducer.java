package zcf.group.MR_time;

import com.aliyun.odps.data.Record;
import com.aliyun.odps.mapred.Reducer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * Reducer模板。请用真实逻辑替换模板内容
 */
public class MyReducer implements Reducer {
    private Record result;

    public void setup(TaskContext context) throws IOException {
        result = context.createOutputRecord();
    } 
    /**
	 * 求相对时间
	 * @param Date1
	 * @return 返回与基准时间(预测日)的相对天数
	 * 静态方法：静态方法不需要创建一个此类的对象即可使用
	 */
    public static long relativeTime(String Date1) {
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	 	try {
	 		Date dt1 = df.parse(Date1);
	 		Date dt2 = df.parse("2014-12-19");

	 		long diff = dt2.getTime() - dt1.getTime();
	 		long days = diff / (1000 * 60 * 60 * 24);
	 		// 如果是预测日，不考虑;只处理观测日之前的数据
//	 		if (days < 0) {
//	 			days = 100;
//	 		}
	 		return days;
	  	} catch (Exception ex) {
	  		ex.printStackTrace();
	  	}
	  	return 100L;
    }
    
    public void reduce(Record key, Iterator<Record> values, TaskContext context) throws IOException {
       long []sum_action = {0, 0, 0, 0, 0}; // 0是总行为数，1是点击，2是收藏，3是加购，4是购买
       boolean isOnlyBefore = false; // 只输出观测值前面的数据
       long day = 0;
        while (values.hasNext()) {
            Record val = values.next();
            
            int behavior_type = val.getBigint("behavior_type").intValue();
            String time = val.getString("time").substring(0, 10);
            day = relativeTime(time);
            if (day > 0 && day < 32) {
            	isOnlyBefore = true;
	        	sum_action[0]++;
	        	switch(behavior_type) {
	        	case 1:
	        		sum_action[1]++;
	        		break;
	        	case 2:
	        		sum_action[2]++;
	        		break; 
	        	case 3:
	        		sum_action[3]++;
	        		break;
	        	case 4:
	        		sum_action[4]++;
	        		break;
	        	}
            }
        }
        
        double time_all_transfer = 0;
        double time_click_transfer = 0;
        double time_cart_colle_transfer = 0;
        if (sum_action[0] > 0) {
        	time_all_transfer = 1.0 * sum_action[4] / sum_action[0];
        }
    	if (sum_action[1] > 0) {
    		time_click_transfer = 1.0 * sum_action[4] / sum_action[1];
    	}
    	if ((sum_action[2] + sum_action[3]) > 0) {
    		time_cart_colle_transfer = 1.0 * sum_action[4] / (sum_action[2] + sum_action[3]);
    	}
    	
        
    	if (isOnlyBefore == true) {
	        result.set(0, key.getBigint("time_flag"));
	        
	        result.set(1, sum_action[0]);
	        result.set(2, sum_action[1]);
	        result.set(3, sum_action[2]);
	        result.set(4, sum_action[3]);
	        result.set(5, sum_action[4]);
	        result.set(6, time_all_transfer);
	        result.set(7, time_click_transfer);        
	        result.set(8, time_cart_colle_transfer);
	
	
	        context.write(result);
    	}
    }

    public void cleanup(TaskContext arg0) throws IOException {

    }
}
