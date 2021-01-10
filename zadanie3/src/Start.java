import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Start implements RemoteConverterInterface {
    private final static String RMI_NAME = "REMOTE_CONVERTER";
    private final AtomicInteger userIdCounter = new AtomicInteger(0);
    private final Map<Integer, UserMeta> users = new ConcurrentHashMap<>();
    private final BlockingQueue<UserMeta> queue = new LinkedBlockingQueue<>();

    public Start() throws RemoteException {
        final RemoteConverterInterface stub =
                (RemoteConverterInterface) UnicastRemoteObject.exportObject(this, 0);
        final Registry registry = LocateRegistry.getRegistry();
        registry.rebind(RMI_NAME, stub);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(RMI_NAME);
            } catch (RemoteException e) {
                throw new RuntimeException();
            } catch (NotBoundException ignored) {
            }
        }));
        System.out.println("Serwer gotowy");
    }

    @Override
    public int registerUser() throws RemoteException {
        final int id = userIdCounter.getAndIncrement();
        users.put(id, new UserMeta(id));
        return id;
    }

    @Override
    public void addDataToList(int userID, int value) throws RemoteException {
        UserMeta user = users.get(userID);
        if (user == null) {
            throw new RemoteException("User o podanym id nie istnieje");
        }
        synchronized (user) {
            if (user.endOfData) {
                throw new RemoteException("User skończył przekazywać dane");
            }
            user.addData(value);
        }
    }

    @Override
    public void setConverterURL(String url) throws RemoteException {
        new Thread(() -> {
            try {
                final ConverterInterface stub =
                        (ConverterInterface) Naming.lookup(url);
                while (true) {
                    UserMeta user = queue.take();
                    user.convertedData = stub.convert(user.data);
                    synchronized (user) {
                        user.resultReady = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void endOfData(int userID) throws RemoteException {
        UserMeta user = users.get(userID);
        if (user == null) {
            throw new RemoteException("User o podanym id nie istnieje");
        }
        synchronized (user) {
            if (user.endOfData) {
                throw new RemoteException("User już wywołał endOfData");
            }
            user.endOfData = true;
            queue.add(user);
        }
    }

    @Override
    public boolean resultReady(int userID) throws RemoteException {
        UserMeta user = users.get(userID);
        if (user == null) {
            throw new RemoteException("User o podanym id nie istnieje");
        }
        synchronized (user) {
            return user.resultReady;
        }
    }

    @Override
    public List<Integer> getResult(int userID) throws RemoteException {
        UserMeta user = users.get(userID);
        if (user == null) {
            throw new RemoteException("User o podanym id nie istnieje");
        }
        synchronized (user) {
            return user.convertedData;
        }
    }

    private class UserMeta {
        final List<Integer> data = new ArrayList<>();
        int userId;
        boolean resultReady = false;
        boolean endOfData = false;
        List<Integer> convertedData = null;

        UserMeta(int userId) {
            this.userId = userId;
        }

        public void addData(int value) {
            this.data.add(value);
        }
    }
}
