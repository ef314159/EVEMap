package EVERender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Scrapes EVE API webpages to read data for kills/jumps in star systems.
 * Extends Thread and runs alongside the render/update thread so that data can
 * be retrieved without interrupting display.
 */
public class APIScraper extends Thread  {
    private Galaxy g;
    private volatile boolean terminate = false;
    
    /**
     * Creates a new instance of APIScraper. Does not attempt to get any data.
     * 
     * @param g the Galaxy to which data should be passed
     */
    public APIScraper (Galaxy g) {
        this.g = g;
    }

    /**
     * The method executed by Thread. Scrapes webpages for data and modifies
     * the instance of Galaxy passed to APIScraper in the constructor.
     */
    @Override public void run() {
        grabJumpData();
        grabKillData();
    }
    
    /**
     * Instructs this thread to terminate as soon as possible.
     */
    public void terminate() {
        terminate = true;
    }
    
    private void grabJumpData() {
        try {
            URL url = new URL("https://api.eveonline.com/map/Jumps.xml.aspx");
            
            BufferedReader in = new BufferedReader (new InputStreamReader(url.openStream()));
        
            String line;
            while ((line = in.readLine()) != null && !terminate) {
                line = line.trim();
                if (line.startsWith("<row ")) {
                    String[] tokens = line.split("\"");
                    
                    g.setJumpsPerHour(
                            Integer.parseInt(tokens[1]),
                            Integer.parseInt(tokens[3]));
                }
            }
                
            in.close();
        } catch (MalformedURLException ex) {
            System.err.println("Malformed URL when scraping jump data.");
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IOException when scraping for jump data.");
            System.err.println(ex.getMessage());
        }
    }
    
    private void grabKillData() {
        try {
            URL url = new URL("https://api.eveonline.com/map/Kills.xml.aspx");
            
            BufferedReader in = new BufferedReader (new InputStreamReader(url.openStream()));
        
            String line;
            while ((line = in.readLine()) != null && !terminate) {
                line = line.trim();
                if (line.startsWith("<row ")) {
                    String[] tokens = line.split("\"");
                    
                    g.setKillsPerHour(
                            Integer.parseInt(tokens[1]),
                            Integer.parseInt(tokens[3]));
                }
            }
            
            in.close();
        } catch (MalformedURLException ex) {
            System.err.println("Malformed URL when scraping kill data.");
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IOException when scraping for kill data.");
            System.err.println(ex.getMessage());
        }
    }
}
