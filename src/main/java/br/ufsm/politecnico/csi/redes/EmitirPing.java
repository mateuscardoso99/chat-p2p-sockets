package br.ufsm.politecnico.csi.redes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.ufsm.politecnico.csi.redes.ChatClientSwing.Usuario;
import br.ufsm.politecnico.csi.redes.Mensagem.TipoMensagem;

/**
 * emite ping pra todos os endereços do arquivo ips.txt
 * também percorre a JList e verifica se o ultimo ping do usuário é maior que 30 segundos, se sim tira da lista
*/
public class EmitirPing implements Runnable{
    private DatagramSocket datagramSocket;
    private List<String> enderecosEnviarPing;
    private JList<?> listaChat;
    private DefaultListModel<Usuario> dfListModel;
    private List<String> enderecosHostLocal;
    private Usuario meuUsuario;

    public EmitirPing(
        DatagramSocket datagramSocket, 
        List<String> enderecosEnviarPing,
        JList<?> listaChat,
        DefaultListModel<Usuario> dfListModel,
        List<String> enderecosHostLocal,
        Usuario meuUsuario
    ){
        this.datagramSocket = datagramSocket;
        this.enderecosEnviarPing = enderecosEnviarPing;
        this.listaChat = listaChat;
        this.dfListModel = dfListModel;
        this.enderecosHostLocal = enderecosHostLocal;
        this.meuUsuario = meuUsuario;
    }

    @Override
    public void run() {           
        while(true){
            enderecosEnviarPing.forEach(endereco -> {
                //verifica se o endereço passado é da própria máquina local, pra não enviar ping pra si mesmo
                if(!enderecosHostLocal.contains(endereco)){
                    try{
                        Mensagem mensagem = new Mensagem();
                        mensagem.setNomeUsuario(meuUsuario.getNome());
                        mensagem.setTipo(TipoMensagem.PING);
                        mensagem.setStatus(meuUsuario.getStatus());

                        ObjectMapper objectMapper = new ObjectMapper();
                        byte[] bytes = objectMapper.writeValueAsBytes(mensagem);

                        DatagramPacket pacote = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(endereco),8084);
                        datagramSocket.send(pacote);
                        System.out.println("PING ENVIADO PARA "+endereco+"\n");
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            for(int i=0;i<listaChat.getModel().getSize();i++){
                Usuario usuario = (Usuario) listaChat.getModel().getElementAt(i);
                if(((System.currentTimeMillis() - usuario.getUltimoPing())/1000) > 30){
                    dfListModel.removeElementAt(i);
                }
            }
                    
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
