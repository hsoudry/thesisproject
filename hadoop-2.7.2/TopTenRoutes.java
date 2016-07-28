import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.lang.String;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class TopTenRoutes {

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
								y[j] = FIRST_CELL_LATITUDE-j*0.004491556;
							}
							int starting_cell_x = getX(pickup_longitude, x), starting_cell_y = getY(pickup_latitude, y), stoping_cell_x = getX(dropoff_longitude, x), stoping_cell_y = getY(dropoff_latitude, y);
							route.set("<<"+Integer.toString(starting_cell_x)+","+Integer.toString(starting_cell_y)+">,<"+Integer.toString(stoping_cell_x)+","+Integer.toString(stoping_cell_y)+">>"); // <<x,y>,<x,y>>
							context.write(route,one);
						}
					}
				}
			}
			catch(ParseException pe) {
				System.err.println("ParseException: "+pe.getMessage());
				System.exit(0);
			}
		}

		private boolean isInGrid(double pickup_longitude, double pickup_latitude, double dropoff_longitude, double dropoff_latitude) {
			if(pickup_longitude<MIN_LONGITUDE || pickup_longitude>MAX_LONGITUDE || pickup_latitude<MIN_LATITUDE || pickup_latitude>MAX_LATITUDE)
				return false;
			if(dropoff_longitude<MIN_LONGITUDE || dropoff_longitude>MAX_LONGITUDE || dropoff_latitude<MIN_LATITUDE || dropoff_latitude>MAX_LATITUDE)
				return false;
			return true;
		}

		private int getX(double l, double[] centers) {
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
		private int getY(double l, double[] centers) {
			int lo = 0;
			int hi = centers.length-1;
			int mid = 1;
			double last_value;
			while(lo<=hi) {
				mid = (lo+hi)/2;
				last_value = centers[mid];
				if(l<last_value)
					lo = mid+1;
				else if (l>last_value)
					hi = mid-1;
				else
					return (mid+1);
			}
			return (mid+1);
		}
	}


	public static class SOTopTenMapper extends Mapper<Text, IntWritable, NullWritable, Text> {
		// Our output key and value Writables
		private TreeMap<Integer, Text> repToRecordMap = new TreeMap<Integer, Text>();

		@Override
		public void map(Text key, IntWritable value, Context context) throws IOException, InterruptedException {
			repToRecordMap.put(new Integer(value.get()), new Text(value.get()+":"+key.toString()));

			if (repToRecordMap.size() > 10) {
				repToRecordMap.remove(repToRecordMap.firstKey());
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException { // is called once per mapper, when all the map functions are finished running
			for(Text value : repToRecordMap.values())
				context.write(NullWritable.get(),value);

		}
	}

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

	public static class SOTopTenReducer extends Reducer<NullWritable, Text, NullWritable, Text> {

		private TreeMap<Integer, Text> repToRecordMap = new TreeMap<Integer, Text>();

		@Override
		public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text value : values) {
				String[] record = value.toString().split(":");
				repToRecordMap.put(Integer.parseInt(record[0]),new Text(record[1]));

				if (repToRecordMap.size() > 10) {
					repToRecordMap.remove(repToRecordMap.firstKey());
				}
			}

			for (Map.Entry<Integer,Text> pair : repToRecordMap.descendingMap().entrySet()) {
				context.write(NullWritable.get(),new Text(pair.getKey().toString()+"	"+pair.getValue().toString()));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		if (args.length != 3) {
			System.err.println("Usage: TopTenRoutes <in> <out> <query_datetime>");
			System.err.println("You have " + args.length + " arguments.");
			System.err.println("They are: ");
			for (int i=0; i<args.length; i++) {
				System.err.println(args[i]);
			}
			System.exit(2);
		}
		conf.set("query_datetime", args[2]); // args[0]
		Path inputPath = new Path(args[0]);
		Path countStage = new Path(args[1]+"_counted");
		Path finalOutput = new Path(args[1]);

		Job job = new Job(conf, "Query 1 Counting Stage");
		job.setJarByClass(TopTenRoutes.class);
		job.setMapperClass(TokenizerMapper.class); // Mapper class
		job.setCombinerClass(IntSumReducer.class); // The combiner adds the similar keys in the same file
		job.setReducerClass(IntSumReducer.class); // Reducer class
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job,inputPath);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, countStage);
		int code = job.waitForCompletion(true) ? 0 : 1;


		if(code == 0) {
			Job top10job = new Job(conf, "Top Ten Most Frequent Routes");
			top10job.setJarByClass(TopTenRoutes.class);
			top10job.setMapperClass(SOTopTenMapper.class);
			top10job.setReducerClass(SOTopTenReducer.class);
			top10job.setNumReduceTasks(1);
			top10job.setOutputKeyClass(NullWritable.class);
			top10job.setOutputValueClass(Text.class);

			//top10job.getConfiguration().set("org.apache.hadoop.mapreduce.lib.output.TextOutputFormat.separator", "");

			top10job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(top10job, countStage);

			FileOutputFormat.setOutputPath(top10job, finalOutput);
			code = top10job.waitForCompletion(true) ? 0 : 2;
		}
		System.exit(code);
	}
}
