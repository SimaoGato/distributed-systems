# Report Fase 3
- Implementámos uma versão simplificada da Arquitetura Gossip para 2 servidores:
    1. O nosso algoritmo não aborda a espera ativa das operações de leitura mandando, assim, uma mensagem de erro.
    2. Não verificamos se uma operação é duplicada, ou seja, tem um timestamp igual a uma operação que já está no update log (ledgerState). 
    3. Quando um dos servidores recebe uma operação do cliente, ele regista como instável e devolve imediatamente uma resposta ao cliente. Após isto, ele verifica imediatamente as condições de domínio e, caso a operação não as respeite tira do ledgerState.
       - Para o utilizador, a única maneira de ver se uma operação foi bem sucedida é fazendo balance.

- As únicas exceções da nossa implementação acontecem quando:
  - Um utilizador está á frente do servidor (verificado pelos Timestamps)
  - Um servidor está inativo (unavailable)
  - Uma conta não existe na tentativa de o user fazer um balance