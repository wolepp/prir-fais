import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class Main {
    public static void main(String[] args) {
        try {
            Server server = new ServerImplementation();
            Naming.rebind("Serwerek", server);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
