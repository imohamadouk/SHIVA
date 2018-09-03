


----- Plateforme Kaa -----
https://kaaproject.github.io/kaa/docs/v0.10.0/Getting-started/

Le serveur Kaa utilisé pour le développement était une kaa sandbox. On peut soit télécharger l'image sur le site et la faire tourner comme VM, soit utiliser AWS (ce qu'on a fini par faire).

Dans les 2 cas, l'interface web est accessible sur le port 8080. Des démos de différentes applications sont disponibles, et on peut configurer/créer de nouvelles applications dans la 'Administration UI'
Il y a 3 niveaux d'utilisateurs : kaa admin, tenant admin et tenant developper. Les identifiants par défaut sont donnés sur la page Getting Started.

Pour créer une nouvelle application, il faut être identifié en tant que tenant admin, et en tant que tenant developper pour la configurer.


----- Créer une application -----

Une fois l'application créée, la configuration consiste à remplir des schémas, soit via l'interface, soit en les donnant directement sous format JSON.
configuration schema permet par exemple de spécifier des délais
data schema spécifie entre autres le format des données envoyées par la fonction de log

Il faut aussi utiliser un log appender : https://kaaproject.github.io/kaa/docs/v0.10.0/Programming-guide/Key-platform-features/Data-collection/

Chaque schéma de config peut avoir plusieurs versions.

Une fois la config faite, pour commencer à coder, on génère un sdk que l'on importe dans le projet.
Ainsi, par exemple, la fonction LOG.info va appeler le log appender spécifié lors de la config
On peut donc changer de système de base de données sans que ça changer quoi ue ce soit dans le code, simplement en remplaçant le sdk par le nouveau.

Pour coder, je suis personnellement parti d'une des démos données dans la sandbox.



----- Vocabulaire important -----

Tout est là normalement: https://kaaproject.github.io/kaa/docs/v0.10.0/Glossary/
Mais ça se mord un peu la queue


Log appender :
Service qui tourne sur la sandbox/serveur (côté Operations Service, cf https://kaaproject.github.io/kaa/docs/v0.10.0/Architecture-overview/ pour tout ça)
Il reçoit les logs des endpoints et les ajoute à une bdd comme spécifié. Il est aussi possible d'utiliser un log appender REST, qui transfère les logs vers un service perso.


Tenant:
un bon dessin https://kaaproject.github.io/kaa/docs/v0.10.0/Architecture-overview/attach/logical-concepts.png
De base, il y a un tenant dans une instance Kaa, et on peut en créer d'autres. Il semble qu'ils servent à isoler différents groupes d'applis qui tournent sur la même instance mais qui n'ont pas besoin d'interagir.


Endpoint:
équipement (terminal ou pas), présent sur le réseau Kaa. Ils peuvent être rassemblés par groupe, ce qui permet d'envoyer des events.
Normalement, une application Kaa tourne sur l'endpoint, le rendant présent dans le réseau Kaa.
Lorsque ce n'est pas possible (pas d'accès au firmware...), une solution est d'utiliser l'architeture actor gateway


Actor Gateway:
Un routeur, sur lequel tourne une application Kaa, et qui se connecte à des endpoints hors de kaa et lance des actors correspondants
Cette architeture est très peu documentée, et il faut tout faire soi-même:
  -un ou des actors, qui tournent comme des threads voire des processus indépendants et qui servent d'interface entre les vrais endpoints et l'application principale
  -un dispatcher, qui tourne sur l'appli principale. Il reconnait la connexion d'un endpoint et lance l'actor correspondant

Meilleure explication trouvée ici : https://jira.kaaproject.org/browse/APP-111

Plus d'info (ou pas):

https://groups.google.com/forum/#!msg/kaaproject/SZBmH8txSbU/qWrpXiuUBQAJ
https://groups.google.com/forum/#!searchin/kaaproject/actor$20gateway/kaaproject/ROeErFjFoOc/uVuYOdVkBAAJ
https://groups.google.com/forum/#!searchin/kaaproject/actor$20gateway/kaaproject/gwuOv3PrxCU/kpNPNa3wCQAJ
https://stackoverflow.com/questions/40814099/building-java-based-kaa-actor-gateway
https://stackoverflow.com/questions/43704357/actor-gateway-for-kaa-server-java-based
