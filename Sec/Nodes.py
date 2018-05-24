#!/usr/bin/env python
# coding: utf-8
from Crypto.Hash import SHA256
from Crypto.Cipher import PKCS1_v1_5
from Crypto.Cipher import PKCS1_OAEP
from Crypto.PublicKey import RSA
from Crypto.Signature import PKCS1_v1_5 as PKCS1_v1_sign
from Crypto import Random
import ast
import base64
import StringIO
import socket
import time

import hashlib
from Crypto.Cipher import AES
BLOCK_SIZE = 16
pad = lambda s: s + (BLOCK_SIZE - len(s) % BLOCK_SIZE) * chr(BLOCK_SIZE - len(s) % BLOCK_SIZE)

def gen_key_pair(passpharse):
    random_generator = Random.new().read
    key = RSA.generate(2048, random_generator)
    return key.exportKey(passphrase=passphrase), key.publickey().exportKey()

# def signature(message, private_key):
#     h = SHA256.new(message)
#     keystream = StringIO.StringIO(private_key)
#     pubkey = RSA.importKey(keystream.read(), passphrase='Your Passphrase')
#     cipher = PKCS1_v1_5.new(pubkey)
#     return base64.encodestring(cipher.encrypt(h.digest()))


def rsa_encrypt(message, recipient_public_key, my_private_key):
    keystream = StringIO.StringIO(recipient_public_key)
    pubkey = RSA.importKey(keystream.read())
    cipher = PKCS1_v1_5.new(pubkey)
    cipher = base64.encodestring(cipher.encrypt(message))
    return cipher

def make_signature(message, key):
    h = SHA256.new(message).digest()
    return h

def aes_encrypt(raw, password, private_key):
    # s = make_signature(raw, private_key)
    session_key = hashlib.sha256(password.encode("utf-8")).digest()
    raw = pad(raw)
    iv = Random.new().read(AES.block_size)
    cipher = AES.new(session_key, AES.MODE_CBC, iv)
    return base64.b64encode(iv + cipher.encrypt(raw))


passphrase = 'shiva'

private_key, public_key = gen_key_pair(passphrase)
s1 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s1.connect(("", 1111))
s1.send(public_key)
server_public_key = s1.recv(9999999)
print(server_public_key)
s1.close()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(("", 2222))
key = "cle de session"
encrypted_key = rsa_encrypt(key, server_public_key, private_key)
s.send(encrypted_key)
s.close()

encrypted = aes_encrypt("Shiva", key, private_key)

s2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
time.sleep(0.5)
s2.connect(("", 3333))
s2.send(encrypted)
s2.close()
