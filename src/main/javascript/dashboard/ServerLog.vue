<template>
  <b-form  v-if="!needApiKey" @submit.stop.prevent="alwaysTrue">
    <button type="submit" class="btn btn-primary" @click="doClearLog">Clear Log</button>
    <log-viewer :log="log"/>
  </b-form>
</template>

<script>
  import {mapGetters, mapState} from 'vuex';
  import axios from 'axios';
  import LogViewer from '@femessage/log-viewer'

  export default {
    name: 'server-log',
    components: {
      'log-viewer': LogViewer
    },
    data () {
      return {
        log: ''
      };
    },
    computed: {
      ...mapState('apiKey', ['apiKey']),
      needApiKey() {
        return this.apiKey == null || this.apiKey.length === 0;
      },
    },
    mounted() {
      const view = this;
      function establishLogListener() {
        const url = new URL('/log/fetch', window.location.href);
        fetch(url.href, {
          method: 'POST',
          mode: 'cors',
          cache: 'no-cache',
          redirect: 'error',
          referrerPolicy: 'no-referrer',
          body: JSON.stringify({
            key: view.apiKey
          })
        }).then(response => {
          var reader = response.body.getReader();
          var decoder = new TextDecoder();

          function readChunk() {
            return reader.read().then(appendChunks);
          }

          function appendChunks(result) {
            var chunk = decoder.decode(result.value || Uint8Array, { stream: !result.done});
            view.log += chunk;
            if (result.done) {
              return "";
            } else {
              return readChunk();
            }
          }

          return readChunk();
        }).then(result => {
          establishLogListener();
        }).catch(err => {
          view.showResult(false, "log read error: " + err);
          setTimeout(establishLogListener, 1000);
        });
      }
      establishLogListener();
    },
    methods: {
      alwaysTrue() {
        return true;
      },
      doClearLog() {
        this.log = ''
      },
      showResult(successValue, messageValue) {
        this.$emit("showResult", {
          message: messageValue,
          success: successValue
        });
      },
    }
  }
</script>

<style lang="scss">
  .server-log {
    min-width: 500px;
    max-width: 100%;
    min-height: 50px;
    height: 100%;
    width: 100%;
  }
</style>
