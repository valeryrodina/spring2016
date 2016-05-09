import poplib, email
# Учетные данные пользователя:
SERVER = "pop.yandex.ru"
USERNAME = "valeriarodina"
USERPASSWORD = "secretword"
p = poplib.POP3(SERVER)
print(p.getwelcome())
# этап идентификации
print(p.user(USERNAME))
print(p.pass_(USERPASSWORD))
# этап транзакций
response, lst, octets = p.list()
print(response)
for msgnum, msgsize in [i.split() for i in lst]:
    print("Сообщение %(msgnum)s имеет длину %(msgsize)s" % vars())
    print("UIDL =", p.uidl(int(msgnum)).split()[2])
if int(msgsize) > 32000:
    (resp, lines, octets) = p.top(msgnum, 0)
else:
    (resp, lines, octets) = p.retr(msgnum)
msgtxt = "\n".join(lines)+"\n\n"
msg = email.message_from_string(msgtxt)
print("* От: %(from)s\n* Кому: %(to)s\n* Тема: %(subject)s\n" % msg)
# msg содержит заголовки сообщения или все сообщение


print(p.quit())