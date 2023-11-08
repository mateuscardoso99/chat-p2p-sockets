package br.ufsm.politecnico.csi.redes;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JList;

import br.ufsm.politecnico.csi.redes.ChatClientSwing.Usuario;

public class Utils {
    /**
     * procura um usuário na JList de usuários pelo endereço de rede passado
     * @param address
     * @param listaChat
     * @return
     */
    public static Usuario buscarUsuarioListModel(InetAddress address, JList<?> listaChat){
        Usuario usuario = null;
        for(int i=0; i<listaChat.getModel().getSize(); i++){
            Usuario u = (Usuario) listaChat.getModel().getElementAt(i);

            if(u.getEndereco().equals(address)){
                usuario = u;
            }
        }
        return usuario;
    }

    /**
     * percorre todas as interfaces de rede do computador
     * e adiciona na lista os ips vinculados a cada uma delas
     * @param endereco
     * @return
     */
    public static List<String> getEnderecosHostLocal() throws SocketException{
        List<String> enderecosLocais = new LinkedList<>();
        Enumeration<NetworkInterface> interfacesRede = NetworkInterface.getNetworkInterfaces();
        while(interfacesRede.hasMoreElements()){
            NetworkInterface i = (NetworkInterface) interfacesRede.nextElement();
            List<InterfaceAddress> enderecosInterface = i.getInterfaceAddresses();
            enderecosLocais.addAll(enderecosInterface.stream().map(add -> add.getAddress().getHostAddress()).toList());
        }
        return enderecosLocais;
    }
}
