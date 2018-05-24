#!/usr/bin/env python
# coding: utf-8
from Crypto.Hash import SHA256
# from Crypto.Signature import PKCS1_v1_5
from Crypto.Cipher import PKCS1_OAEP
from Crypto.Cipher import PKCS1_v1_5
from Crypto.PublicKey import RSA
# from Crypto.Hash import SHA
from Crypto import Random
import ast
import base64
import StringIO
import socket
import threading
import time
global message, i
i = 1
message = {"key":"", "ciphertext":""}

import hashlib
from Crypto.Cipher import AES
BLOCK_SIZE = 16
unpad = lambda s: s[:-ord(s[len(s) - 1:])]

class ClientThread(threading.Thread):

    def __init__(self, ip, port, clientsocket):

        threading.Thread.__init__(self)
        self.ip = ip
        self.port = port
        self.clientsocket = clientsocket
        print("[+] Nouveau thread pour %s %s" % (self.ip, self.port, ))

    def run(self):

        print("Connection de %s %s" % (self.ip, self.port, ))

        received = self.clientsocket.recv(2048)
        # message = ""
        # print("J'ai recu : ", received)
        global message
        global i
        if i == 1:
            message["key"] = received
            i+=1
        elif i == 2:
            message["ciphertext"] = received

        # message = message+received


class KeyThread(threading.Thread):

    def __init__(self, ip, port, clientsocket):

        threading.Thread.__init__(self)
        self.ip = ip
        self.port = port
        self.clientsocket = clientsocket
        print("[+] Echange de clés pour %s %s" % (self.ip, self.port, ))

    def run(self):

        print("Connection de %s %s" % (self.ip, self.port, ))
        client_public_key = self.clientsocket.recv(2048)
        print("J'ai recu la clé publique du client: ", client_public_key)
        global passphrase
        passphrase = 'Your Passphrase'
        random_generator = Random.new().read
        key = RSA.generate(2048, random_generator)
        global private_key
        private_key = key.exportKey(passphrase=passphrase)
        public_key = key.publickey().exportKey()

        self.clientsocket.send(public_key)

def rsa_decrypt(ciphertext, my_private_key, passphrase):
    ciphertext = base64.decodestring(ciphertext)
    keystream = StringIO.StringIO(my_private_key)
    pemkey = RSA.importKey(keystream.read(), passphrase=passphrase)
    dsize = SHA256.digest_size
    sentinel = Random.new().read(20+dsize)
    cipher = PKCS1_v1_5.new(pemkey)
    message = cipher.decrypt(ciphertext, sentinel)
    return message

def aes_decrypt(enc, password):
    x = len(enc)
    private_key = hashlib.sha256(password.encode("utf-8")).digest()
    enc = base64.b64decode(enc)
    iv = enc[:16]
    cipher = AES.new(private_key, AES.MODE_CBC, iv)
    return unpad(cipher.decrypt(enc[16:]))


tcpsock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcpsock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
tcpsock.bind(("",1111))

tcpsock.listen(10)
print( "En écoute...")
(clientsocket, (ip, port)) = tcpsock.accept()
keythread = KeyThread(ip, port, clientsocket)
keythread.start()

tcpsock1 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcpsock1.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
tcpsock1.bind(("",2222))
tcpsock1.listen(10)
print( "En écoute...")
(clientsocket1, (ip1, port1)) = tcpsock1.accept()
clientthread = ClientThread(ip1, port1, clientsocket1)
clientthread.start()

tcpsock2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcpsock2.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
tcpsock2.bind(("",3333))
tcpsock2.listen(10)
print( "En écoute...")
(clientsocket2, (ip2, port2)) = tcpsock2.accept()
clientthread2 = ClientThread(ip2, port2, clientsocket2)
clientthread2.start()

key = rsa_decrypt(message["key"], private_key, passphrase)

decrypted = aes_decrypt(message["ciphertext"], key)
Plaintext = bytes.decode(decrypted)
print("decrypted message = "+ Plaintext)
