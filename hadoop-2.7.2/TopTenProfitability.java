import java.io.IOException;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.lang.String;
import java.util.TreeMap;
import java.io.DataInput;
import java.io.DataOutput;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.io.SortedMapWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapred.lib.IdentityMapper;

public class TopTenProfitability {
  public static final int NUMBER_OF_ATTRIBUTES = 17;
  public static final int GRID_SIZE = 600;
	public static final double MIN_LATITUDE = 40.129715978;
	public static final double MAX_LATITUDE = 41.477182778;
	public static final double MIN_LONGITUDE = -74.916578;
	public static final double MAX_LONGITUDE = -73.120778;
	public static final double FIRST_CELL_LATITUDE = 41.476059889;
	public static final double FIRST_CELL_LONGITUDE = -74.9150815;

  public static class MedianMapper extends Mapper<Object, Text, Text, SortedMapWritable> {
    private DoubleWritable price = new DoubleWritable();
    private Text starting_cell = new Text();
    private static final LongWritable ONE = new LongWritable(1);
    private Calendar query_datetime = null;
    private Calendar query_lowerbound = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    double[] x = new double[GRID_SIZE];
    double[] y = new double[GRID_SIZE];

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
      try {
        query_datetime = new GregorianCalendar();
        Date d = simpleDateFormat.parse(context.getConfiguration().get("query_datetime"));
        query_datetime.setTime(d);
        query_lowerbound = (GregorianCalendar)query_datetime.clone();
        query_lowerbound.add(Calendar.MINUTE, -15);
        for(int j=0;j<GRID_SIZE;j++) {
          x[j] = FIRST_CELL_LONGITUDE+j*(0.005986/2);
          y[j] = FIRST_CELL_LATITUDE-j*(0.004491556/2);
        }
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      try {
        String[] parameters = value.toString().split(",");
        int n = parameters.length;
        if(n == NUMBER_OF_ATTRIBUTES) {
          String dropoff_datetime_string = parameters[3];
          Calendar dropoff_datetime = new GregorianCalendar();
          Date d = simpleDateFormat.parse(dropoff_datetime_string);
          dropoff_datetime.setTime(d);
          if(dropoff_datetime.after(query_lowerbound) && dropoff_datetime.before(query_datetime)) {
            double pickup_longitude = Double.parseDouble(parameters[6]), pickup_latitude = Double.parseDouble(parameters[7]);
            if(TopTenProfitability.isInGrid(pickup_longitude, pickup_latitude)) {
              int area_x = TopTenProfitability.getX(pickup_longitude, x), area_y = TopTenProfitability.getY(pickup_latitude, y);
              double fee = Double.parseDouble(parameters[11])+Double.parseDouble(parameters[14]);
              starting_cell.set("<"+area_x+","+area_y+">");
              price.set(fee);
              SortedMapWritable outPrice = new SortedMapWritable();
              outPrice.put(price,ONE);
              context.write(starting_cell,outPrice);
            }
          }
        }
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }
  }

  public static class MedianReducer extends Reducer<Text, SortedMapWritable, Text, DoubleWritable> {
    private TreeMap<Double, Long> medianValues = new TreeMap<Double, Long>();
    private DoubleWritable median = new DoubleWritable();

    @Override
    public void reduce(Text key, Iterable<SortedMapWritable> values, Context context) throws IOException, InterruptedException {
      long totalTrips = 0;
      median.set(0.);
      medianValues.clear();

      for (SortedMapWritable v : values) {
        for (Map.Entry<WritableComparable, Writable> entry : v.entrySet()) {
          double price = ((DoubleWritable)entry.getKey()).get();
          long count = ((LongWritable)entry.getValue()).get();
          totalTrips+=count;
          Long storedCount = medianValues.get(price);
          if(storedCount == null)
            medianValues.put(price,count);
          else
            medianValues.put(price, storedCount+count);
        }
        v.clear();
      }

      long medianIndex = totalTrips / 2L;
      long previousTrips = 0;
      long trips = 0;
      double prevKey = 0;
      for (Map.Entry<Double,Long> entry : medianValues.entrySet()) {
        trips = previousTrips + entry.getValue();
        if (previousTrips <= medianIndex && medianIndex < trips) {
          if(totalTrips % 2 == 0 && previousTrips == medianIndex)
            median.set((entry.getKey()+prevKey)/2.0);
          else
            median.set(entry.getKey());
          break;
        }
        previousTrips = trips;
        prevKey = entry.getKey();
      }
      context.write(key, median);
    }
  }

  public static class MedianCombiner extends Reducer<Text, SortedMapWritable, Text, SortedMapWritable> {

    @Override
    protected void reduce(Text key, Iterable<SortedMapWritable> values, Context context) throws IOException, InterruptedException {
      SortedMapWritable outValue = new SortedMapWritable();
      for (SortedMapWritable v : values) {
        for (Map.Entry<WritableComparable, Writable> entry : v.entrySet()) {
          LongWritable count = (LongWritable)outValue.get(entry.getKey());
          if(count != null)
            count.set(count.get()+((LongWritable)entry.getValue()).get());
          else
            outValue.put(entry.getKey(), new LongWritable(((LongWritable)entry.getValue()).get()));
        }
        v.clear();
      }
      context.write(key, outValue);
    }
  }

  public static class EmptyTaxisMapper extends Mapper<Object, Text, Text, Text> {
    private Text medallion = new Text();
    private Calendar query_datetime = null;
    private Calendar query_lowerbound = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
      try {
        query_datetime = new GregorianCalendar();
        Date d = simpleDateFormat.parse(context.getConfiguration().get("query_datetime"));
        query_datetime.setTime(d);
        query_lowerbound = (GregorianCalendar)query_datetime.clone();
        query_lowerbound.add(Calendar.MINUTE, -30);
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      try {
        String[] parameters = value.toString().split(",");
        int n = parameters.length;
        if(n == NUMBER_OF_ATTRIBUTES) {
          Calendar pickup_datetime = new GregorianCalendar(), dropoff_datetime = new GregorianCalendar();
          pickup_datetime.setTime(simpleDateFormat.parse(parameters[2]));
          dropoff_datetime.setTime(simpleDateFormat.parse(parameters[3]));
          if ((pickup_datetime.after(query_lowerbound) && pickup_datetime.before(query_datetime)) || (dropoff_datetime.after(query_lowerbound) && dropoff_datetime.before(query_datetime))) {
            medallion.set(parameters[0]);
            context.write(medallion,value);
          }
        }
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }
  }

  public static class EmptyTaxisReducer extends Reducer<Text, Text, Text, IntWritable> {
    private Calendar query_datetime = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Text dropoff_cell = new Text();
    private static final IntWritable one = new IntWritable(1);
    private double[] x = new double[GRID_SIZE];
    private double[] y = new double[GRID_SIZE];

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
      try {
        query_datetime = new GregorianCalendar();
        Date d = simpleDateFormat.parse(context.getConfiguration().get("query_datetime"));
        query_datetime.setTime(d);
        for (int i=0; i<GRID_SIZE; i++) {
          x[i] = FIRST_CELL_LONGITUDE+i*(0.005986/2);
          y[i] = FIRST_CELL_LATITUDE-i*(0.004491556/2);
        }
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
      try {
        Text savedRecord = new Text();
        GregorianCalendar savedDropoff_datetime = new GregorianCalendar(), currentDropoff_datetime = new GregorianCalendar();
        savedDropoff_datetime.setTimeInMillis(0);
        String[] parameters;
        for(Text record : values) {
          parameters = record.toString().split(",");
          currentDropoff_datetime.setTime(simpleDateFormat.parse(parameters[3]));
          if(currentDropoff_datetime.after(savedDropoff_datetime)) {
            savedDropoff_datetime = (GregorianCalendar)currentDropoff_datetime.clone();
            savedRecord.set(record);
          }
        }
        if(!(savedDropoff_datetime.after(query_datetime))) {
          parameters = savedRecord.toString().split(",");
          double dropoff_longitude = Double.parseDouble(parameters[8]);
          double dropoff_latitude = Double.parseDouble(parameters[9]);
          if(TopTenProfitability.isInGrid(dropoff_longitude, dropoff_latitude)) {
            dropoff_cell.set("<"+TopTenProfitability.getX(dropoff_longitude, x)+","+TopTenProfitability.getY(dropoff_latitude, y)+">");
            context.write(dropoff_cell, one);
          }
        }
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }
  }

  public static class EmptyTaxisCombiner extends Reducer<Text, Text, Text, Text> {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
      try {
        Text savedRecord = new Text();
        GregorianCalendar savedDropoff_datetime = new GregorianCalendar(), currentDropoff_datetime = new GregorianCalendar();
        savedDropoff_datetime.setTimeInMillis(0);
        String[] parameters;
        for(Text record : values) {
          parameters = record.toString().split(",");
          currentDropoff_datetime.setTime(simpleDateFormat.parse(parameters[3]));
          if(currentDropoff_datetime.after(savedDropoff_datetime)) {
            savedDropoff_datetime = (GregorianCalendar)currentDropoff_datetime.clone();
            savedRecord.set(record);
          }
        }
        context.write(key,savedRecord);
      }
      catch(ParseException pe) {
        System.err.println("ParseException: "+pe.getMessage());
        System.exit(0);
      }
    }
  }

  public static class EmptyTaxisCountMapper extends Mapper<Text, IntWritable, Text, AreaStats> {

    @Override
    public void map(Text key, IntWritable value, Context context) throws IOException, InterruptedException {
      AreaStats as = new AreaStats();
      as.set(0.,value.get());
      context.write(key, as);
    }
  }

  public static class EmptyTaxisCountReducer extends Reducer<Text, AreaStats, Text, AreaStats> {

    @Override
    public void reduce(Text key, Iterable<AreaStats> values, Context context) throws IOException, InterruptedException {
      AreaStats as = new AreaStats();
      for (AreaStats val : values) {
        as.set(0., as.getNumberOfEmptyTaxis().get()+val.getNumberOfEmptyTaxis().get());
      }
      context.write(key, as);
    }
  }

  public static class MergeMedianMapper extends Mapper<Text, DoubleWritable, Text, AreaStats> {
    private AreaStats as = new AreaStats();

    @Override
    public void map(Text key, DoubleWritable value, Context context) throws IOException, InterruptedException {
      as.set(value, new IntWritable(0));
      context.write(key, as);
    }
  }

  public static class MergeReducer extends Reducer<Text, AreaStats, Text, AreaStats> {

    @Override
    public void reduce(Text key, Iterable<AreaStats> values, Context context) throws IOException, InterruptedException {
      AreaStats as = new AreaStats();
      for (AreaStats val : values) {
        as.set(as.getMedian().get()+val.getMedian().get(), as.getNumberOfEmptyTaxis().get()+val.getNumberOfEmptyTaxis().get());
      }
      context.write(key,as);
    }
  }

  public static class TopTenMapper extends Mapper<Text, AreaStats, NullWritable, Text> {
    private TreeMap<Double, Text> topTenMap = new TreeMap<Double, Text>();

    @Override
    public void map(Text key, AreaStats value, Context context) throws IOException, InterruptedException {
      topTenMap.put(Double.parseDouble(value.getProfitability().toString()),new Text(value.getProfitability().toString()+":"+key.toString()+"\t"+value.getNumberOfEmptyTaxis().toString()+"\t"+value.getMedian().toString()+"\t"+value.getProfitability().toString()));

      if(topTenMap.size()>10)
        topTenMap.remove(topTenMap.firstKey());
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
      for(Text t : topTenMap.values())
        context.write(NullWritable.get(),t);
    }
  }

  public static class TopTenReducer extends Reducer<NullWritable, Text, NullWritable, Text> {
    private TreeMap<Double, Text> topTen = new TreeMap<Double, Text>();

    @Override
    public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
      for (Text v : values) {
        String[] areaDescription = v.toString().split(":");
        topTen.put(Double.parseDouble(areaDescription[0]), new Text(areaDescription[1]));
        if(topTen.size() > 10)
          topTen.remove(topTen.firstKey());
      }

      for (Map.Entry<Double,Text> pair : topTen.descendingMap().entrySet()) {
				context.write(NullWritable.get(),new Text(pair.getValue().toString()));
			}
    }
  }

  public static boolean isInGrid(double longitude, double latitude) {
    if(longitude<MIN_LONGITUDE || longitude>MAX_LONGITUDE || latitude<MIN_LATITUDE || latitude>MAX_LATITUDE)
      return false;
    return true;
  }

  public static int getX(double l, double[] centers) {
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

  public static int getY(double l, double[] centers) {
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

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    if (args.length != 3) {
  		System.err.println("Usage: TopTenProfitability <in> <out> <query_datetime>");
  		System.err.println("You have " + args.length + " arguments.");
  		System.err.println("They are: ");
  		for (int i=0; i<args.length; i++)
  			System.err.println(args[i]);
  		System.exit(2);
  	}

    conf.set("query_datetime", args[2]);
  	Path inputPath = new Path(args[0]);
    Path medianOutput = new Path(args[1]+"_median");
    Path emptyTaxisOutput = new Path(args[1]+"_emptytaxis");
    Path totalEmptyTaxisOutput = new Path(args[1]+"_totalemptytaxis");
    Path mergeOutput = new Path(args[1]+"_merge");
  	Path finalOutput = new Path(args[1]);

    Job medianJob = new Job(conf, "Median computation job");
    medianJob.setJarByClass(TopTenProfitability.class);
    medianJob.setMapperClass(MedianMapper.class);
    medianJob.setReducerClass(MedianReducer.class);
    medianJob.setCombinerClass(MedianCombiner.class);
    FileInputFormat.addInputPath(medianJob,inputPath);
    medianJob.setOutputKeyClass(Text.class);
    medianJob.setMapOutputValueClass(SortedMapWritable.class);
    medianJob.setOutputValueClass(DoubleWritable.class);
    medianJob.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(medianJob,medianOutput);
    int codeMedian = medianJob.waitForCompletion(true) ? 0 : 1;

    Job emptyTaxisLocationJob = new Job(conf, "Location of empty taxis job");
    emptyTaxisLocationJob.setJarByClass(TopTenProfitability.class);
    emptyTaxisLocationJob.setMapperClass(EmptyTaxisMapper.class);
    emptyTaxisLocationJob.setReducerClass(EmptyTaxisReducer.class);
    emptyTaxisLocationJob.setCombinerClass(EmptyTaxisCombiner.class);
    FileInputFormat.addInputPath(emptyTaxisLocationJob,inputPath);
    emptyTaxisLocationJob.setOutputKeyClass(Text.class);
    emptyTaxisLocationJob.setMapOutputValueClass(Text.class);
    emptyTaxisLocationJob.setOutputValueClass(IntWritable.class);
    // FileOutputFormat.setOutputPath(emptyTaxisLocationJob,emptyTaxisOutput);
    emptyTaxisLocationJob.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(emptyTaxisLocationJob,emptyTaxisOutput);
    int codeTaxis = emptyTaxisLocationJob.waitForCompletion(true) ? 0 : 1;

    if(codeTaxis == 0) {
      Job emptyTaxisJob = new Job(conf, "Number of empty taxis per cell job");
      emptyTaxisJob.setJarByClass(TopTenProfitability.class);
      emptyTaxisJob.setMapperClass(EmptyTaxisCountMapper.class);
      emptyTaxisJob.setReducerClass(EmptyTaxisCountReducer.class);
      emptyTaxisJob.setCombinerClass(EmptyTaxisCountReducer.class);
      emptyTaxisJob.setInputFormatClass(SequenceFileInputFormat.class);
      SequenceFileInputFormat.addInputPath(emptyTaxisJob, emptyTaxisOutput);
      emptyTaxisJob.setOutputKeyClass(Text.class);
      emptyTaxisJob.setOutputValueClass(AreaStats.class);
      //FileOutputFormat.setOutputPath(emptyTaxisJob, totalEmptyTaxisOutput);
      emptyTaxisJob.setOutputFormatClass(SequenceFileOutputFormat.class);
      SequenceFileOutputFormat.setOutputPath(emptyTaxisJob,totalEmptyTaxisOutput);
      int codeTotalTaxis = emptyTaxisJob.waitForCompletion(true) ? 0 : 1;

      if ((codeTotalTaxis == 0)&&(codeMedian == 0)) {
        Job mergeJob = new Job(conf, "Merge of the two results job");
        mergeJob.setJarByClass(TopTenProfitability.class);
        MultipleInputs.addInputPath(mergeJob, medianOutput, SequenceFileInputFormat.class, MergeMedianMapper.class);
        MultipleInputs.addInputPath(mergeJob, totalEmptyTaxisOutput, SequenceFileInputFormat.class, Mapper.class);
        mergeJob.setReducerClass(MergeReducer.class);
        mergeJob.setOutputKeyClass(Text.class);
        mergeJob.setOutputValueClass(AreaStats.class);
        // FileOutputFormat.setOutputPath(mergeJob, mergeOutput);
        mergeJob.setOutputFormatClass(SequenceFileOutputFormat.class);
        SequenceFileOutputFormat.setOutputPath(mergeJob,mergeOutput);
        int codeMerge = mergeJob.waitForCompletion(true) ? 0 : 1;

        if(codeMerge == 0) {
          Job topTenJob = new Job(conf, "Top ten areas");
          topTenJob.setJarByClass(TopTenProfitability.class);
          topTenJob.setMapperClass(TopTenMapper.class);
          topTenJob.setNumReduceTasks(1);
          topTenJob.setReducerClass(TopTenReducer.class);
          topTenJob.setInputFormatClass(SequenceFileInputFormat.class);
          SequenceFileInputFormat.addInputPath(topTenJob,mergeOutput);
          topTenJob.setOutputKeyClass(NullWritable.class);
          topTenJob.setOutputValueClass(Text.class);
          FileOutputFormat.setOutputPath(topTenJob, finalOutput);
          int code = topTenJob.waitForCompletion(true) ? 0 : 1;

          System.exit(code);
        }
      }
    }

    System.exit(codeTaxis);
  }

  public static class AreaStats implements WritableComparable<AreaStats> {
    private DoubleWritable median;
    private IntWritable numberOfEmptyTaxis;
    private DoubleWritable profitability;

    public AreaStats() {
      profitability = new DoubleWritable(0.);
      set(new DoubleWritable(), new IntWritable());
    }

    public AreaStats(double median, int numberOfEmptyTaxis) {
      profitability = new DoubleWritable(0.);
      set(new DoubleWritable(median), new IntWritable(numberOfEmptyTaxis));
    }

    public AreaStats(DoubleWritable median, IntWritable numberOfEmptyTaxis) {
      profitability = new DoubleWritable(0.);
      set(median, numberOfEmptyTaxis);
    }

    public void set(double median, int numberOfEmptyTaxis) {
      this.median = new DoubleWritable(median);
      this.numberOfEmptyTaxis = new IntWritable(numberOfEmptyTaxis);
      if(this.numberOfEmptyTaxis.get() != 0)
        this.profitability = new DoubleWritable(median/(double)numberOfEmptyTaxis);
      else
        this.profitability.set(0.);
    }

    public void set(DoubleWritable median, IntWritable numberOfEmptyTaxis) {
      this.median = median;
      this.numberOfEmptyTaxis = numberOfEmptyTaxis;
      if(this.numberOfEmptyTaxis.get() != 0)
        this.profitability.set(this.median.get() / (double)this.numberOfEmptyTaxis.get());
      else
        this.profitability.set(0.);
    }

    public DoubleWritable getMedian() {
      return median;
    }

    public IntWritable getNumberOfEmptyTaxis() {
      return numberOfEmptyTaxis;
    }

    public DoubleWritable getProfitability() {
      return profitability;
    }

    public void clear() {
      median.set(0.);
      numberOfEmptyTaxis.set(0);
      profitability.set(0.);
    }

    @Override
    public void write(DataOutput out) throws IOException {
      median.write(out);
      numberOfEmptyTaxis.write(out);
      profitability.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
      median.readFields(in);
      numberOfEmptyTaxis.readFields(in);
      profitability.readFields(in);
    }

    @Override
    public int hashCode() {
      return median.hashCode()*163+numberOfEmptyTaxis.hashCode()*67+profitability.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if(o instanceof AreaStats) {
        AreaStats as = (AreaStats)o;
        return median.equals(as.median) && numberOfEmptyTaxis.equals(as.numberOfEmptyTaxis) && profitability.equals(as.profitability);
      }
      return false;
    }

    @Override
    public String toString() {
      return "Median: "+median.toString()+", Number of empty taxis: "+numberOfEmptyTaxis.toString()+", Profitability: "+profitability.toString();
    }

    @Override
    public int compareTo(AreaStats as) {
      int cmp = median.compareTo(as.median);
      if(cmp != 0)
        return cmp;
      else {
        cmp = numberOfEmptyTaxis.compareTo(as.numberOfEmptyTaxis);
        if(cmp != 0)
          return cmp;
      }
      return profitability.compareTo(as.profitability);
    }
  }
}
