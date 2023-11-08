package br.ufsm.politecnico.csi.redes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.ufsm.politecnico.csi.redes.ChatClientSwing.StatusUsuario;
import br.ufsm.politecnico.csi.redes.ChatClientSwing.Usuario;

/**
 * thread que fica ouvindo ping de outros usuarios
 * verifica se o usuário que enviou o ping está na JList de usuários,
 * se estiver atualiza o timestamp do ultimo ping dele, se não tiver adiciona na JList
*/
public class RecebePing implements Runnable{
    private DatagramSocket datagramSocket;
    private JList<?> listaChat;
    private DefaultListModel<Usuario> dfListModel;

    public RecebePing(DatagramSocket datagramSocket,DefaultListModel<Usuario> dfListModel,JList<?> listaChat){
        this.datagramSocket = datagramSocket;
        this.listaChat = listaChat;
        this.dfListModel = dfListModel;
    }

    @Override
    public void run() {
        while(true){
            try {
                byte[] bytes = new byte[4096];

                DatagramPacket pacoteRecebido = new DatagramPacket(bytes, bytes.length);
                datagramSocket.receive(pacoteRecebido);

                Mensagem mensagem = new ObjectMapper().readValue(pacoteRecebido.getData(),0,pacoteRecebido.getLength(), Mensagem.class);

                System.out.println("---- PING RECEBIDO: "+new String(pacoteRecebido.getData(),StandardCharsets.UTF_8)+" RECEBIDO DE " + pacoteRecebido.getAddress()+ " USUARIO: "+mensagem.getNomeUsuario()+"\n");

                Usuario usuarioExiste = Utils.buscarUsuarioListModel(pacoteRecebido.getAddress(), listaChat);

                if(usuarioExiste != null){
                    usuarioExiste.setUltimoPing(System.currentTimeMillis());
                    usuarioExiste.setNome(mensagem.getNomeUsuario());
                    usuarioExiste.setStatus(mensagem.getStatus());
                }
                else{
                    Usuario u = new Usuario(mensagem.getNomeUsuario(), StatusUsuario.ONLINE, pacoteRecebido.getAddress(), System.currentTimeMillis());
                    dfListModel.addElement(u);
                }
                listaChat.invalidate();
                listaChat.repaint();                   
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }   
}
