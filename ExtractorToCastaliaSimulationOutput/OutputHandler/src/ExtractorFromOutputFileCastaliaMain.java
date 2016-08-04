import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExtractorFromOutputFileCastaliaMain {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) throws Exception
	{
		HashMap<String, Float> lastValuesTime = new HashMap<>();
		
		String path = "/home/gustavo/omnetpp-4.6/Castalia_bck/Simulations/leach/saida.txt";
		
		// TODO Auto-generated method stub
		
//		if(args.length != 0)
//		{
//			path = args[0];
//		}
//		else
//			throw new Exception("Favor inserir parâmetros - pathfile do arquivo de log (formato .txt)!");

		FileReader input = new FileReader(path);
		BufferedReader fileReaded = new BufferedReader(input);
		
		String line = fileReaded.readLine();
		String[] result = {};
		
		Pattern p = Pattern.compile("([+-]?\\d*\\.\\d+).*?(\\[.*?\\])", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher m;
		
		while(line != null)
		{
			try
			{
				m = p.matcher(line);
				m.find();
				//System.out.println("ID: "+m.group(2)+", Value: "+m.group(1));
				if(!lastValuesTime.containsKey(m.group(2)))
				{
					lastValuesTime.put(m.group(2), Float.valueOf(m.group(1)));
				}
				else
				{
					if(lastValuesTime.get(m.group(2))< Float.valueOf(m.group(1)))
						lastValuesTime.put(m.group(2), Float.valueOf(m.group(1)));
				}
				
			}
			catch(Exception e)
			{
				System.out.println(line);
				e.printStackTrace();
			}
			finally
			{
				line = fileReaded.readLine();
			}
		}
		
		for(String key : lastValuesTime.keySet())
		{
			System.out.println("ID: "+key+", ValueUpdate: "+lastValuesTime.get(key.toString()));
		}
		
		
	}

}
