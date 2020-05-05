package com.ioliveira.ecommerce.services;

import com.ioliveira.ecommerce.controllers.dto.request.ClienteInsertDTO;
import com.ioliveira.ecommerce.controllers.dto.request.ClienteRequestDTO;
import com.ioliveira.ecommerce.controllers.dto.response.ClienteResponseDTO;
import com.ioliveira.ecommerce.entities.Cidade;
import com.ioliveira.ecommerce.entities.Cliente;
import com.ioliveira.ecommerce.entities.Endereco;
import com.ioliveira.ecommerce.entities.enums.TipoCliente;
import com.ioliveira.ecommerce.repositories.CidadeRepository;
import com.ioliveira.ecommerce.repositories.ClienteRepository;
import com.ioliveira.ecommerce.repositories.EnderecoRepository;
import com.ioliveira.ecommerce.services.exceptions.DataIntegrityException;
import com.ioliveira.ecommerce.services.exceptions.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CidadeRepository cidadeRepository;
    @Autowired
    private EnderecoRepository enderecoRepository;

    public List<ClienteResponseDTO> findAll() {
        List<Cliente> clienteList = clienteRepository.findAll();
        return clienteList
                .stream()
                .map(ClienteResponseDTO::new)
                .collect(Collectors.toList());
    }

    public Cliente findById(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Objeto não encontrado! ID: " + id));
    }

    public Page<ClienteResponseDTO> findPage(Integer page, Integer linesPerPage, String direction, String orderBy) {
        PageRequest pageRequest = PageRequest.of(page, linesPerPage, Sort.Direction.valueOf(direction), orderBy);
        return clienteRepository.findAll(pageRequest).map(ClienteResponseDTO::new);
    }

    public void update(Integer id, ClienteRequestDTO requestDTO) {
        Cliente clienteDB = findById(id);
        clienteUpdated(clienteDB, requestDTO.getNome(), requestDTO.getEmail());
    }

    public void deleteById(Integer id) {
        findById(id);
        try {
            clienteRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityException("Não é possível deletar um Cliente que possui pedidos associados.");
        }
    }

    private void clienteUpdated(Cliente cliente, String nomeCliente, String email) {
        cliente.setNome(nomeCliente);
        cliente.setEmail(email);
        clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente insert(ClienteInsertDTO insertDTO) {
        Cliente cliente = convertToEntity(insertDTO);
        enderecoRepository.saveAll(cliente.getEnderecos());
        try {
            return clienteRepository.save(cliente);
        } catch (DataIntegrityViolationException e){
            //Atributo email da entidade Cliente anotado com @Column(unique = true)
            //Nao permite repeticoes. Chave unica
            throw new DataIntegrityException("Email já cadastrado!");
        }
    }

    private Cliente convertToEntity(ClienteInsertDTO insertDTO) {
        Cliente cliente = new Cliente(insertDTO.getNome(), insertDTO.getEmail(), insertDTO.getCpfCnpj(),
                TipoCliente.toEnum(insertDTO.getTipoCliente()));

        Cidade cidade = findCidadeById(insertDTO.getCidadeId());

        Endereco endereco = new Endereco(insertDTO.getLogradouro(), insertDTO.getNumero(),
                insertDTO.getComplemento(), insertDTO.getBairro(), insertDTO.getCep(), cliente, cidade);

        cliente.getEnderecos().add(endereco);
        cliente.setTelefones(insertDTO.getTelefones());

        return cliente;
    }

    private Cidade findCidadeById(Integer id) {
        return cidadeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Objeto não encontrado! ID: " + id));
    }

}
