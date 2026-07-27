# language: pt
@e2e @compensacao
Funcionalidade: Compensação da SAGA de pagamento
  Para impedir a execução de uma ordem sem pagamento confirmado
  Como plataforma PitFlow
  Quero cancelar a ordem quando o pagamento for rejeitado

  Cenário: Cancelar a ordem quando o pagamento for rejeitado
    Dado que os dados acadêmicos de teste estão disponíveis
    E que uma nova ordem de serviço aguarda pagamento
    Quando o pagamento dessa ordem for rejeitado
    Então o Payment deve permanecer em "REJECTED"
    E a Operation deve chegar a "CANCELLED"
    E a SAGA deve terminar em "FAILED"
    Quando a mesma rejeição for reenviada
    Então a rejeição deve ser idempotente
