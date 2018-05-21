def server(type, title) {
    def cis = configurationApi.searchByTypeAndTitle(type, title)
    if (cis.isEmpty()) {
        throw new RuntimeException("No CI found for the type '${type}' and title '${title}'")
    }
    if (cis.size() > 1) {
        throw new RuntimeException("More than one CI found for the type '${type}' and title '${title}'")
    }
    cis.get(0)
}

def ispwServiceServer1 = server('ispwServices.Server','CWC2')
def ispwServiceServer2 = server('ispwServices.Server','CWC2')
def ispwServiceServer3 = server('ispwServices.Server','CWC2')
def ispwServiceServer4 = server('ispwServices.Server','CWC2')


xlr {
  release('Release from Jenkins') {
    variables {
      stringVariable('ISPW_QA_DEPL_TASK_ID') {
        required false
        showOnReleaseStart false
      }
      stringVariable('ISPW_RELEASE_ID') {
        required true
        label 'ISPW Release ID'
        description 'Release to Deploy'
        showOnReleaseStart false
      }
      stringVariable('ISPW_PREPROD_DEPL_TASK_ID') {
        required false
        showOnReleaseStart false
      }
      stringVariable('XL_Promote_task') {
        required false
        showOnReleaseStart false
        label 'XL_Promote_task'
        description 'Task to wait for'
      }
      stringVariable('ISPW_TASK_URL') {
        required false
        showOnReleaseStart false
        label 'ISPW_TASK_URL'
      }
      stringVariable('XL_Deploy_task') {
        required false
        showOnReleaseStart false
        label 'XL_Deploy_task'
      }
      stringVariable('ISPW_Dev_level') {
        required false
        showOnReleaseStart false
        label 'ISPW_Dev_level'
        value 'DEV1'
      }
      stringVariable('ISPW_STG_Level') {
        required false
        showOnReleaseStart false
        label 'ISPW_STG_Level'
        value 'DEV1'
      }
      stringVariable('xebiauser') {
        required true
        showOnReleaseStart false
        label 'xebiauser'
        description 'username of xebia'
        value 'admin'
      }
      passwordVariable('xebiapassword') {
        required true
        label 'password'
        description 'password for the xebia password'
        value '{b64}9ohABj+D4HdiX3TXRvDTzg=='
      }
      stringVariable('ISPW_Token') {
        required true
        label 'ISPW_Token'
        showOnReleaseStart false
      }
    }
    description 'ISPW Release'
    scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2018-10-23T11:00:00-0400')
    scriptUsername 'admin'
    scriptUserPassword '{b64}9ohABj+D4HdiX3TXRvDTzg=='
    phases {
      phase('QA Environment') {
        scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2019-03-27T11:00:00-0400')
        dueDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-30T13:00:00-0400')
        color '#009CDB'
        tasks {
          gate('Ready for QA') {
            description 'Run the deployment process'
          }
          custom('XL Release Control') {
            owner 'admin'
            team 'Approvers Team'
            script {
              type 'xlr.GetTaskId'
              taskTitle 'Wait for Promote'
              taskId variable('XL_Promote_task')
            }
          }
          parallelGroup('Accept Code promotion to QA') {
            tasks {
              custom('Promote to QA') {
                owner 'admin'
                scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-27T12:00:00-0400')
                plannedDuration 600
                waitForScheduledStartDate false
                script {
                  type 'ispwServices.Promote'
                  ispwServiceServer ispwServiceServer1
                  cesToken variable('ISPW_Token')
                  srid 'cwc2.nasa.cpwr.corp'
                  runtimeConfiguration 'ISP8'
                  callbackTaskId '${XL_Promote_task}'
                  callbackUrl 'http://dtw-xebialabs-cwc2:5516'
                  callbackUsername '${xebiauser}'
                  callbackPassword variable('xebiapassword')
                  relId variable('ISPW_RELEASE_ID')
                  level 'STG1'
                  autoDeploy false
                  setId variable('ISPW_QA_DEPL_TASK_ID')
                  url variable('ISPW_TASK_URL')
                }
              }
              manual('Wait for Promote') {
                owner 'admin'
              }
            }
          }
          custom('XL Release Control') {
            owner 'admin'
            script {
              type 'xlr.GetTaskId'
              taskTitle 'Wait for Deploy'
              taskId variable('XL_Deploy_task')
            }
          }
          parallelGroup('Mainframe Deployment') {
            tasks {
              custom('Deploy to QA LPARs') {
                owner 'admin'
                scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-27T12:00:00-0400')
                plannedDuration 120
                waitForScheduledStartDate false
                script {
                  type 'ispwServices.Deploy'
                  ispwServiceServer ispwServiceServer2
                  cesToken variable('ISPW_Token')
                  srid 'cwc2.nasa.cpwr.corp'
                  runtimeConfiguration 'ISP8'
                  callbackTaskId '${XL_Deploy_task}'
                  callbackUrl 'http://dtw-xebialabs-cwc2:5516'
                  callbackUsername '${xebiauser}'
                  callbackPassword variable('xebiapassword')
                  relId variable('ISPW_RELEASE_ID')
                  level 'QA'
                  setId variable('XL_Promote_task')
                  url variable('ISPW_TASK_URL')
                }
              }
              manual('Wait for Deploy') {
                owner 'admin'
              }
            }
          }
          gate('QA Testing Complete') {
            description 'Run the deployment process'
          }
        }
      }
      phase('Production Environment') {
        scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-27T11:00:00-0400')
        dueDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-30T13:00:00-0400')
        color '#009CDB'
        tasks {
          custom('XL Release Control') {
            owner 'admin'
            script {
              type 'xlr.GetTaskId'
              taskTitle 'Wait for Promote'
              taskId variable('XL_Promote_task')
            }
          }
          parallelGroup('Request Code promotion to Production') {
            tasks {
              custom('Promote to Prod') {
                owner 'admin'
                scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-27T12:00:00-0400')
                plannedDuration 600
                waitForScheduledStartDate false
                script {
                  type 'ispwServices.Promote'
                  ispwServiceServer ispwServiceServer3
                  cesToken variable('ISPW_Token')
                  srid 'cwc2.nasa.cpwr.corp'
                  runtimeConfiguration 'ISP8'
                  callbackTaskId '${XL_Promote_task}'
                  callbackUrl 'http://dtw-xebialabs-cwc2:5516'
                  callbackUsername '${xebiauser}'
                  callbackPassword variable('xebiapassword')
                  relId variable('ISPW_RELEASE_ID')
                  level 'QA'
                  autoDeploy false
                  setId variable('ISPW_QA_DEPL_TASK_ID')
                  url variable('ISPW_TASK_URL')
                }
              }
              manual('Wait for Promote') {
                owner 'admin'
              }
            }
          }
          custom('XL Release Control') {
            owner 'admin'
            script {
              type 'xlr.GetTaskId'
              taskTitle 'Wait for Deploy'
              taskId variable('XL_Deploy_task')
            }
          }
          parallelGroup('Mainframe Deployment') {
            tasks {
              custom('Deploy to PROD LPARs') {
                owner 'admin'
                scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2017-03-27T12:00:00-0400')
                plannedDuration 120
                waitForScheduledStartDate false
                script {
                  type 'ispwServices.Deploy'
                  ispwServiceServer ispwServiceServer4
                  cesToken variable('ISPW_Token')
                  srid 'cwc2.nasa.cpwr.corp'
                  runtimeConfiguration 'ISP8'
                  callbackTaskId '${XL_Deploy_task}'
                  callbackUrl 'http://dtw-xebialabs-cwc2:5516'
                  callbackUsername '${xebiauser}'
                  callbackPassword variable('xebiapassword')
                  relId variable('ISPW_RELEASE_ID')
                  level 'PRD'
                  setId variable('XL_Promote_task')
                  url variable('ISPW_TASK_URL')
                }
              }
              manual('Wait for Deploy') {
                owner 'admin'
              }
            }
          }
        }
      }
    }
    teams {
      team('Template Owner') {
        members 'admin'
        permissions 'template#edit', 'template#view', 'template#edit_triggers', 'template#edit_security', 'template#create_release'
      }
      team('Release Admin') {
        members 'admin'
        permissions 'release#edit', 'template#edit', 'release#reassign_task', 'template#edit_security', 'template#create_release', 'release#edit_security', 'release#view', 'release#start', 'release#edit_blackout', 'template#view', 'template#edit_triggers', 'release#abort', 'release#edit_task'
      }
      team('Approvers Team') {
        roles 'Approvers'
        permissions 'release#reassign_task', 'release#start', 'release#abort', 'release#edit_task'
      }
    }
    extensions {
      dashboard {
        tiles {
          releaseProgressTile('Release progress') {
            
          }
          timelineTile('Release timeline') {
            row 2
          }
          releaseHealthTile('Release health') {
            
          }
          releaseSummaryTile('Release summary') {
            
          }
          iSPWTile('ISPW Deploys') {
            row 1
            col 0
            action 'Deploy'
          }
          iSPWTile('ISPW Promotes') {
            row 1
            col 1
            action 'Promote'
          }
        }
      }
    }
  }
}
