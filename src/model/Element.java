package model;

public class Element {
    private Addresse adr;
    private String etat;
    private String modif;

    public Element(int i, int j) {
        this.adr = new Addresse(i,j);
        etat="intact";
        modif=" # ";
    }

    public Element(int i, int j,String s) {
        this.adr = new Addresse(i,j);
        etat=s;
    }

    public Element() {
        etat="intact";
    }

    public Element(String motif) {

        etat="intact";
        this.modif = motif;
    }

    public int getabcisse(){

        return this.adr.getAdrLigne();
    }
    public int getordonnee(){

        return this.adr.getAdrColone();
    }
    public boolean toucheR(Addresse b) {

        boolean a= false;
        if(this.adr.equal(b)) {
            if(etat=="intact"){
                etat="abime";
                this.modif=" * ";
                System.out.println("toucher");
                a=true;
            }if(etat=="detruuit"){
                System.out.println("Déjà détruit");
            }else{
                System.out.println("Déjà abimé");
            }
        }
        return a;
    }

    public boolean detruit(){
        this.setEtat("detruit");
        this.modif=" ! ";
        return true;
    }

    public void avancer(int i,int j){
        if((i>=-1) && (i<=1) && (j>=-1) && (j<=1)){
            this.adr.setAdrLigne(this.adr.getAdrLigne()+i);
            this.adr.setAdrColone(this.adr.getAdrColone()+j);
        }
    }

    public String getEtat(){

        return etat;
    }

    public void setEtat(String a){

        etat=a;
    }

    public String toString() {
        return this.modif;
    }

    public void setAdresse(Addresse b) {

        this.adr = b;
    }
    public Addresse getAdresse() {

        return  this.adr;
    }

}
