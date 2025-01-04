<template>
  <b-form v-if="!needApiKey" @submit.stop.prevent="alwaysTrue"><div class="form-group">
    <VueCodeEditor
        v-model="content"
        @init="editorInit"
        lang="javascript"
        theme="monokai"
        width="100%"
        height="200px"
        :options="{
            //enableBasicAutocompletion: true,
            //enableLiveAutocompletion: false,
            fontSize: 14,
            highlightActiveLine: true,
            //enableSnippets: false,
            showLineNumbers: true,
            tabSize: 2,
            showPrintMargin: false,
            showGutter: true,
        }"
        :commands="[
            {
                name: 'save',
                bindKey: { win: 'Ctrl-s', mac: 'Command-s' },
                exec: null,
                readOnly: true,
            },
        ]"
    />
    <button type="submit" class="btn btn-primary" @click="doClearScript">Clear Script</button>
    <button type="submit" class="btn btn-primary" @click="doClearOutput">Clear Output</button>
    <button type="submit" class="btn btn-primary" @click="doExecute" v-if="!inProgress">Execute Script</button>
    <button type="submit" class="btn btn-primary" v-if="inProgress" disabled>
      <b-spinner small/> Stopping...
    </button>
    <output-viewer v-if="!needApiKey" :log="output"/>
  </div></b-form>
</template>

<script>
  import {mapGetters, mapState} from 'vuex';
  import axios from 'axios';
  import VueCodeEditor from 'vue2-code-editor';
  import LogViewer from '@femessage/log-viewer'

  export default {
    name: 'scripting',
    components: {
      VueCodeEditor,
      'output-viewer': LogViewer,
    },
    data () {
      return {
        inProgress: false,
        content: '',
        output: 'Empty',
        count: 0,
      };
    },
    computed: {
      ...mapState('apiKey', ['apiKey']),
      needApiKey() {
        return this.apiKey == null || this.apiKey.length === 0;
      },
    },
    methods: {
      alwaysTrue() {
        return true;
      },
      doExecute() {
        console.log("doExecute")
        this.inProgress = true;
        this.output += "\nexecute...";
        axios.post('/quartz/execute', {
          key: this.apiKey,
          task: {
            name: "no-name-" + ++this.count,
            code: this.content,
          }
        }).then(response => {
          this.inProgress = false;
          if (response.data.result) {
            //console.log("response = " + JSON.stringify(response.data));
            this.output += "\nsuccess: " + response.data.result.success + "\n";
            if (response.data.result.success) {
              this.output += response.data.result.output;
            } else {
              this.output = JSON.stringify(response.data.result, null, 2);
            }
          } else {
            this.output += "unparsable: " + JSON.stringify(response.data, null, 2)
          }
          this.output += "\n";
        }).catch(error => {
          if (error.response) {
            this.output += "error: " + error.response.data + "\n";
            this.showResult(false, error.response.data);
          } else {
            this.output += "error: " + error.message + "\n";
            this.showResult(false, error.message);
          }
          this.inProgress = false;
        })

      },
      showResult(successValue, messageValue) {
        this.$emit("showResult", {
          message: messageValue,
          success: successValue
        });
      },
      doClearScript() {
        this.content = '';
      },
      doClearOutput() {
        this.output = '';
      },
      dataSubmit() {
      },
      editorInit: function () {
            require('brace/ext/language_tools') //language extension prerequsite...
            //require('brace/mode/html') //language
            require('brace/mode/javascript')
            //require('brace/mode/less')
            require('brace/theme/monokai')
            require('brace/snippets/javascript') //snippet
      },
    }
  }
</script>
