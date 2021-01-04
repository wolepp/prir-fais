import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    public static void main(String[] args) {
        try {
            Server srv = (Server) Naming.lookup("rmi://" + args[0] + "/Serwerek");
            System.out.println(srv.sum(20, 44));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
