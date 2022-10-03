encrypt: ansible-vault encrypt vars.yml
decrypt: ansible-vault decypt vars.yml
edit: ansible-vault edit vars.yml
change pwd: ansible-vault rekey vars.yml

be prompted for password: --ask-vault-pass

upload to server: ansible-playbook main.yml --ask-vault-pass
