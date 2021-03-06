package testSocket;

import game.Game;
import game.Joueur;
import model.Addresse;
import model.Element;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class ClientProcessor implements Runnable{

    private Socket sock;
    private PrintWriter writer = null;
    private BufferedInputStream reader = null;
    private Game realGame;
    private Joueur joueur;

    public ClientProcessor(Socket pSock, Game agame , Joueur joueur){
        sock = pSock;
        realGame = agame;
        this.joueur= joueur;
    }

    public ClientProcessor(Socket pSock) {
        sock = pSock;
    }

    public Game getRealGame() {
        return realGame;
    }

    public void setRealGame(Game realGame) {
        this.realGame = realGame;
    }

    //Le traitement lancé dans un thread séparé
    public void run(){
        System.err.println("Lancement du traitement de la connexion cliente");

        boolean closeConnexion = false;
        //tant que la connexion est active, on traite les demandes
        while(!sock.isClosed()){

            try {

                //Ici, nous n'utilisons pas les mêmes objets que précédemment
                //Je vous expliquerai pourquoi ensuite
                writer = new PrintWriter(sock.getOutputStream());
                reader = new BufferedInputStream(sock.getInputStream());

                //On attend la demande du client
                String response = read();
                InetSocketAddress remote = (InetSocketAddress)sock.getRemoteSocketAddress();

                //On affiche quelques infos, pour le débuggage
                String debug = "";
                debug = "Thread : " + Thread.currentThread().getName() + ". ";
                debug += "Attaque sur l'adresse : " + remote.getAddress().getHostAddress() +".";
                debug += "\t -> Commande reçue : " + response + "\n";
                System.err.println("\n" + debug);

                //On traite la demande du client en fonction de la commande envoyée

                String toSend = "";
                if(response !=null && response !=" ") {
                    String a = response;
                    String[] tab = a.split(",");
                    System.out.println("LANCE ATTTAQUE");
                    System.out.println(tab[0]);
                    System.out.println(tab[1]);
                    int x = Integer.valueOf(tab[0]);
                    int y = Integer.valueOf(tab[1]);
                    System.out.println("LANCE ATTTAQUE");
                    int test = realGame.lanceAttaque(new Addresse(x, y));
                    switch (test) {
                        case 0:
                            System.out.println("A l'eau ");
                            toSend = "2, A l'eau !";
                            int score = realGame.getJoueurs().get(joueur.getIdGame()).getScore();
                            toSend += " votre score ( " + joueur.getLogin() + ") est de : " + score + "\n";
                            realGame.setMessageAttaque(toSend);
                            if (realGame.getProchainJoueur() == realGame.getJoueurs().size() - 1) {
                                realGame.setProchainJoueur(0);
                            } else {
                                realGame.setProchainJoueur(joueur.getIdGame() + 1);
                            }
                            toSend += "Prochain joueur, " + realGame.getProchainJoueur();
                            break;
                        case 1:
                            System.out.println("toucher");
                            realGame.getFlotte().getMaps().setElementT(new Element(" * "),new Addresse(x,y));
                            toSend = "1, Toucher ! \n ";
                            realGame.getJoueurs().get(joueur.getIdGame()).setScore(realGame.getJoueurs().get(joueur.getIdGame()).getScore() + 1);
                            score = realGame.getJoueurs().get(joueur.getIdGame()).getScore();
                            realGame.setMessageAttaque(toSend);
                            if (realGame.getProchainJoueur() == realGame.getJoueurs().size() - 1) {
                                realGame.setProchainJoueur(0);
                            } else {
                                if (realGame.getJoueurs().size() == 1) {
                                    realGame.setProchainJoueur(0);
                                } else {
                                    realGame.setProchainJoueur(joueur.getIdGame() + 1);
                                }
                            }
                            toSend += "Prochain joueur, " + realGame.getProchainJoueur();
                            break;
                        case 2:
                            System.out.println("Couller");
                            realGame.getFlotte().getMaps().setElementT(new Element(" ! "),new Addresse(x,y));
                            toSend = "3, Couller"+realGame.getFlotte().getBateauDetruis().getClass()+","+realGame.getFlotte().getBateauDetruis().getPlace()+", Couler ! \n ";
                            realGame.getJoueurs().get(joueur.getIdGame()).setScore(realGame.getJoueurs().get(joueur.getIdGame()).getScore() + 3);
                            score = realGame.getJoueurs().get(joueur.getIdGame()).getScore();
                            realGame.setMessageAttaque(toSend);
                            if (realGame.getProchainJoueur() == realGame.getJoueurs().size() - 1) {
                                realGame.setProchainJoueur(0);
                            } else {
                                if (realGame.getJoueurs().size() == 1) {
                                    realGame.setProchainJoueur(0);
                                } else {
                                    realGame.setProchainJoueur(joueur.getIdGame() + 1);
                                }
                            }
                            toSend += "Prochain joueur, " + realGame.getProchainJoueur();
                            break;
                        default:
                            toSend += "Jouer SVP , " + realGame.getProchainJoueur();
                            break;
                    }
                    response = null;
                }
                //On envoie la réponse au client
                writer.write(toSend);
                //Il FAUT IMPERATIVEMENT UTILISER flush()
                //Sinon les données ne seront pas transmises au client
                //et il attendra indéfiniment
                writer.flush();
                if(closeConnexion){
                    System.err.println("COMMANDE CLOSE DETECTEE ! ");
                    writer = null;
                    reader = null;
                    sock.close();
                    break;
                }
            }catch(SocketException e){
                System.err.println("LA CONNEXION A ETE INTERROMPUE ! ");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //La méthode que nous utilisons pour lire les réponses
    private String read() throws IOException{
        String response = "";
        int stream;
        byte[] b = new byte[9096];
        stream = reader.read(b);
        response = new String(b, 0, stream);
        return response;
    }
}
