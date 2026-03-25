declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
    generatorType?: 'AI_STRATEGY' | 'HTML' | 'MULTI_FILE' | 'VUE_PROJECT'
  }

  type AppAdminUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppCodeGenRequest = {
    appId?: number
    message?: string
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    lastCreateTime?: string
    message?: string
    messageType?: string
    appId?: number
    userId?: number
  }

  type chatToGenCodeParams = {
    appCodeGenRequest: AppCodeGenRequest
  }

  type chatToGenCodeStreamParams = {
    appId: number
    message: string
    isWorkflow?: boolean
  }

  type deleteByIdParams = {
    id: number
  }

  type DeleteRequest = {
    id?: number
  }

  type downloadCodeZipFileParams = {
    appId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listAppChatHistoryByPageParams = {
    appId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    createTime?: string
    updateTime?: string
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type resetUserPasswordParams = {
    userId: number
  }

  type ServerSentEventString = true

  type uploadAppCoverParams = {
    appId: number
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdatePasswordRequest = {
    userId?: number
    oldPassword?: string
    newPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    createTime?: string
  }

  type viewAppParams = {
    appId: number
  }

  // ===== PromptTemplate Types =====

  type PromptTemplateVO = {
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type PromptTemplateAddRequest = {
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
  }

  type PromptTemplateUpdateRequest = {
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
  }

  type PromptTemplateQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    isEnabled?: number
  }

  type BaseResponsePromptTemplateVO = {
    code?: number
    data?: PromptTemplateVO
    message?: string
  }

  type BaseResponseListPromptTemplateVO = {
    code?: number
    data?: PromptTemplateVO[]
    message?: string
  }

  type PagePromptTemplateVO = {
    records?: PromptTemplateVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePagePromptTemplateVO = {
    code?: number
    data?: PagePromptTemplateVO
    message?: string
  }

  // ===== SystemPrompt Types =====

  type SystemPromptVO = {
    id?: number
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type SystemPromptAddRequest = {
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
  }

  type SystemPromptUpdateRequest = {
    id?: number
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
  }

  type SystemPromptQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    promptName?: string
    promptKey?: string
  }

  type BaseResponseSystemPromptVO = {
    code?: number
    data?: SystemPromptVO
    message?: string
  }

  type BaseResponseListSystemPromptVO = {
    code?: number
    data?: SystemPromptVO[]
    message?: string
  }

  type PageSystemPromptVO = {
    records?: SystemPromptVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageSystemPromptVO = {
    code?: number
    data?: PageSystemPromptVO
    message?: string
  }

  // ===== PromptTemplate Field Definition =====

  type TemplateField = {
    type: 'select' | 'text' | 'switch'
    key: string
    label: string
    options?: string[]
    defaultValue?: any
    trueText?: string
    falseText?: string
  }

  // ===== Social Module Types =====

  type AppHotStatVO = {
    appId?: number
    likeCount?: number
    shareCount?: number
    favoriteCount?: number
    commentCount?: number
    hasLiked?: boolean
    hasFavorited?: boolean
  }

  type BaseResponseAppHotStatVO = {
    code?: number
    data?: AppHotStatVO
    message?: string
  }

  type AppCommentVO = {
    id?: number
    appId?: number
    userId?: number
    content?: string
    likeCount?: number
    createTime?: string
    user?: UserVO
    hasLiked?: boolean
  }

  type AppCommentAddRequest = {
    appId?: number
    content?: string
  }

  type AppCommentQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    appId?: number
  }

  type PageAppCommentVO = {
    records?: AppCommentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppCommentVO = {
    code?: number
    data?: PageAppCommentVO
    message?: string
  }

  type AppFavoriteQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type AppFavoriteRecord = {
    id?: number
    appId?: number
    userId?: number
    createTime?: string
  }

  type PageAppFavoriteRecord = {
    records?: AppFavoriteRecord[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppFavoriteRecord = {
    code?: number
    data?: PageAppFavoriteRecord
    message?: string
  }

  type AppHotStat = {
    id?: number
    appId?: number
    likeCount?: number
    shareCount?: number
    favoriteCount?: number
    commentCount?: number
    createTime?: string
    updateTime?: string
  }

  type PageAppHotStat = {
    records?: AppHotStat[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppHotStat = {
    code?: number
    data?: PageAppHotStat
    message?: string
  }
}
