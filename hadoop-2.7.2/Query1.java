import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

//import com.sun.org.apache.xml.internal.utils.StopParseException;

public class Query1 {

	public static final int NUMBER_OF_ATTRIBUTES = 17;
	public static final int GRID_SIZE = 300;
	public static final double MIN_LATITUDE = 40.129715978;
	public static final double MAX_LATITUDE = 41.477182778;
	public static final double MIN_LONGITUDE = -74.916578;
	public static final double MAX_LONGITUDE = -73.120778;
	public static final double FIRST_CELL_LATITUDE = 41.474937;
	public static final double FIRST_CELL_LONGITUDE = -74.913585;

	public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text route = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException { // The input text is one line
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // date format used by the input file
				// we get the datetime from the user input
				Configuration conf = context.getConfiguration(); 
				Calendar query_datetime = new GregorianCalendar();
				Date d = simpleDateFormat.parse(conf.get("query_datetime"));
				query_datetime.setTime(d);
				
				Calendar query_lowerbound = (GregorianCalendar)query_datetime.clone();
				query_lowerbound.add(Calendar.MINUTE, -30); // creation of the 30 minutes window
				
				String[] parameters = value.toString().split(",");
				int n = parameters.length; // n should always be 17
				
				if(n==NUMBER_OF_ATTRIBUTES) {
					String pickup_datetime_string = parameters[2];
					String dropoff_datetime_string = parameters[3];
					Calendar dropoff_datetime = new GregorianCalendar();
					d = simpleDateFormat.parse(dropoff_datetime_string);
					dropoff_datetime.setTime(d);
					if(dropoff_datetime.after(query_lowerbound) && dropoff_datetime.before(query_datetime)) {						
						double pickup_longitude = Double.parseDouble(parameters[6]), pickup_latitude = Double.parseDouble(parameters[7]), dropoff_longitude = Double.parseDouble(parameters[8]), dropoff_latitude = Double.parseDouble(parameters[9]);
						
						if(isInGrid(pickup_longitude,pickup_latitude,dropoff_longitude,dropoff_latitude)) {
							double[] x = new double[GRID_SIZE], y = new double[GRID_SIZE];
							for(int j=0;j<GRID_SIZE;j++) {
								x[j] = FIRST_CELL_LONGITUDE+j*0.005986;
								y[j] = FIRST_CELL_LATITUDE+j*0.004491556;
							}
							int starting_cell_x = getCell(pickup_longitude, x), starting_cell_y = getCell(pickup_latitude, y), stoping_cell_x = getCell(dropoff_longitude, x), stoping_cell_y = getCell(dropoff_latitude, y);
							route.set("<<"+Integer.toString(starting_cell_x)+","+Integer.toString(starting_cell_y)+">,<"+Integer.toString(stoping_cell_x)+","+Integer.toString(stoping_cell_y)+">>"); // <<x,y>,<x,y>>
							context.write(route,one);
						}
					}
				}
			}
			catch (ParseException pe) {
				System.err.println("Error: "+pe.getMessage());
				System.exit(0);
			}
		}

		private boolean isInGrid(double pickup_longitude, double pickup_latitude, double dropoff_longitude, double dropoff_latitude) {
			if(pickup_longitude<MIN_LONGITUDE || pickup_longitude>MAX_LONGITUDE || pickup_latitude<MIN_LATITUDE || pickup_latitude>MAX_LATITUDE) 
				return false;
			if(dropoff_longitude<MIN_LONGITUDE || dropoff_longitude>MAX_LONGITUDE || dropoff_latitude<MIN_LATITUDE || dropoff_latitude>MAX_LATITUDE) {
				return false;
			}
			return true;
		}

		private int getCell(double l, double[] centers) {
			int lo = 0;
			int hi = centers.length-1;
			int mid = 1;
			double last_value;
			while(lo<=hi) {
				mid = (lo+hi)/2;
				last_value = centers[mid];
				if(l<last_value)
					hi = mid-1;
				else if (l>last_value)
					lo = mid+1;
				else
					return (mid+1);	
			}
			return (mid+1);
		}

	}

	// At the end of the mapping process, we get < <<x,y>,<x,y>>, 1> pairs.
	// We get one map "output" for each file.

	public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Configuration conf = new Configuration();
			conf.set("query_datetime","2013-01-01 00:29:00"); // args[0]
			Job job = Job.getInstance(conf, "Query 1"); 
			job.setJarByClass(Query1.class);
			job.setMapperClass(TokenizerMapper.class); // Mapper class
			job.setCombinerClass(IntSumReducer.class); // The combiner adds the similar keys in the same file
			job.setReducerClass(IntSumReducer.class); // Reducer class
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(IntWritable.class);
			FileInputFormat.addInputPath(job, new Path(args[0]));
			FileOutputFormat.setOutputPath(job, new Path(args[1]));
			System.exit(job.waitForCompletion(true) ? 0 : 1);
			
		}
		catch(Exception e) {
			System.err.println("Exception caught: "+e.getMessage());
		}
	}
}

