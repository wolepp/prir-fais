import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ServerImplementation extends UnicastRemoteObject implements Server {

    ServerImplementation() throws RemoteException {
        super();
    }

    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
