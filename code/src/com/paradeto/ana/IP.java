package com.paradeto.ana;
import com.paradeto.pro.Process;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

/**
 * 统计网站日志的总流量及IP数
 * @author youxingzhi
 *
 */
public class IP {
	private static long bytesperday = 0;
	private static int ipsperday = 0;
    public static class KPIIPMapper extends MapReduceBase implements Mapper<Object, Text, Text, Text> {
        private Text bytes = new Text();
        private Text ips = new Text();

        @Override
        public void map(Object key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
        	String line = value.toString();
        	Process kpi = Process.parser(line);
        	//统计爬虫
            if (kpi.isValid()&&kpi.isSpider()) {
                ips.set(kpi.getRemote_addr());
                bytes.set(kpi.getBody_bytes_sent());
                output.collect(ips, bytes);
            }
            //统计所有
            //只需稍微修改爬虫的代码，略
            //统计非爬虫
            //只需稍微修改爬虫的代码，略
        }
    }

    public static class KPIIPReducer extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        private Text result = new Text();
        

        @Override
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
        	long bytesperip = 0;
        	while (values.hasNext()) {
                bytesperip = bytesperip + (Long.parseLong(values.next().toString()));
            }
        	bytesperday = bytesperday+bytesperip;
        	ipsperday++;
            result.set(String.valueOf(bytesperip));
            output.collect(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        String input = "hdfs://master:9000/logdata/";
        String output = "hdfs://master:9000/ips-bytes-day-ipspiders";

        JobConf conf = new JobConf(IP.class);
        conf.setJobName("IP");
        //conf.addResource("classpath:/hadoop/core-site.xml");
        //conf.addResource("classpath:/hadoop/hdfs-site.xml");
        //conf.addResource("classpath:/hadoop/mapred-site.xml");
        
        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(Text.class);
        
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);
        
        conf.setMapperClass(KPIIPMapper.class);
        //conf.setCombinerClass(KPIIPReducer.class);
        conf.setReducerClass(KPIIPReducer.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(input));
        FileOutputFormat.setOutputPath(conf, new Path(output));

        JobClient.runJob(conf);
        System.out.println("Request filter spiders:");
        System.out.println("IPs per day:");
        System.out.println(ipsperday);
        System.out.println("Bytes per day:");
        System.out.println(bytesperday);
        System.exit(0);
    }

}
